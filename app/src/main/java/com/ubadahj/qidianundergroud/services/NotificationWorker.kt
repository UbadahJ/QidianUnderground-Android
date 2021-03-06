package com.ubadahj.qidianundergroud.services

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.github.ajalt.timberkt.e
import com.ubadahj.qidianundergroud.MainActivity
import com.ubadahj.qidianundergroud.R
import com.ubadahj.qidianundergroud.models.Book
import com.ubadahj.qidianundergroud.models.ChapterGroup
import com.ubadahj.qidianundergroud.models.Metadata
import com.ubadahj.qidianundergroud.repositories.BookRepository
import com.ubadahj.qidianundergroud.repositories.ChapterGroupRepository
import com.ubadahj.qidianundergroud.repositories.MetadataRepository
import com.ubadahj.qidianundergroud.utils.models.lastChapter
import com.ubadahj.qidianundergroud.utils.repositories.getGroups
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class NotificationWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val bookRepo = BookRepository(context)
    private val groupRepo = ChapterGroupRepository(context)
    private val metaRepo = MetadataRepository(context)

    private val notificationId = 42069

    override suspend fun doWork(): Result {
        createNotificationChannel(applicationContext)
        getNotifications().collect {
            with(NotificationManagerCompat.from(applicationContext)) {
                notify(it.hashCode(), it.createNotification())
            }
        }
        return Result.success()
    }

    private fun getNotifications() = flow {
        val books = bookRepo.getBooks(true).first()
            .filter { it.inLibrary }
            .associateWith { metaRepo.getBook(it).first() }

        progressNotification(books) { book, metadata ->
            try {
                val lastGroup = book.getGroups(applicationContext).first()
                val refreshedGroups = groupRepo.getGroups(book, true).first()
                val updateCount = refreshedGroups.lastChapter() - lastGroup.lastChapter()
                if (updateCount > 0 && metadata?.enableNotification == true)
                    emit(
                        BookNotification(
                            applicationContext, book, metadata, refreshedGroups, updateCount
                        )
                    )
            } catch (e: Exception) {
                e(e) { "getNotifications: Failed to query $book" }
            }
        }
    }.flowOn(Dispatchers.IO)

    private fun List<ChapterGroup>.lastChapter(): Int {
        return maxByOrNull { it.lastChapter }?.lastChapter ?: 0
    }

    private suspend fun progressNotification(
        books: Map<Book, Metadata?>,
        action: suspend (Book, Metadata?) -> Unit
    ) {
        val builder = NotificationCompat.Builder(applicationContext, CHANNEL_ID).apply {
            setContentTitle("Checking for updates")
            setSmallIcon(R.drawable.app_image_outline)
            setOnlyAlertOnce(true)
            priority = NotificationCompat.PRIORITY_LOW
        }

        NotificationManagerCompat.from(applicationContext).apply {
            builder.setProgress(books.size, 0, false)
            notify(notificationId, builder.build())

            books.onEachIndexed { counter, (book, metadata) ->
                builder.apply {
                    setContentText(book.name)
                    setProgress(books.size, counter, false)
                    notify(notificationId, build())
                }

                action(book, metadata)
            }

            builder.setProgress(0, 0, false)
            cancel(notificationId)
        }
    }

    private data class BookNotification(
        val context: Context,
        val book: Book,
        val metadata: Metadata?,
        val groups: List<ChapterGroup>,
        val updateCount: Int
    ) {

        private val lastGroup = groups.maxByOrNull { it.lastChapter }!!
        private val text = "$updateCount new chapter${if (updateCount > 1) "s" else ""} available"

        private val groupKey = "com.ubadahj.qidianunderground.CHAPTER_UPDATES"

        suspend fun createNotification(): Notification =
            NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.app_image_outline)
                .setContentTitle(book.name)
                .setContentText(text)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(createIntent(lastGroup))
                .addAction(R.drawable.add, "Open Book", createIntent())
                .setAutoCancel(true)
                .setLargeIcon(context, metadata?.coverPath)
                .setGroup(groupKey)
                .build()

        private fun createIntent(group: ChapterGroup? = null) =
            PendingIntent.getActivity(
                context,
                hashCode() + group.hashCode(),
                Intent(context, MainActivity::class.java).apply {
                    putExtra("book", book.id)
                    putExtra("group", group?.link)
                },
                PendingIntent.FLAG_UPDATE_CURRENT
            )!!

    }

}
