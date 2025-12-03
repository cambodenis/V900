package com.example.v900.alerts

/*
class AlertActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_ALERT_TYPE = "extra_alert_type"
        const val EXTRA_MESSAGE = "extra_message"
        const val EXTRA_METADATA = "extra_metadata"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Включаем поведение: когда activity снова откроется, экран включится и покажется поверх блокировки
        window.addFlags(
            android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )

        val type = intent.getStringExtra(EXTRA_ALERT_TYPE) ?: "GENERAL"
        val msg = intent.getStringExtra(EXTRA_MESSAGE) ?: ""
        val meta = intent.getStringExtra(EXTRA_METADATA)

        setContent {
            AlertScreen(
                type = type,
                message = msg,
                metadata = meta,
                onDismiss = {
                    finish()
                },
                onOpenApp = {
                    // Открыть основное activity (если нужно)
                    val open = Intent(this, MainActivity::class.java)
                    open.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    startActivity(open)
                    finish()
                },
                onSnooze = {
                    // отправим intent сервису отложить на 5 минут — либо обработаем локально
                    // здесь просто завершаем activity; сервис может хранить snooze в DataStore
                    finish()
                },
                onMute = {
                    // выключить все звуки — можно отправить Broadcast или сохранить в repository
                    finish()
                }
            )
        }
    }
}

@Composable
fun AlertScreen(
    type: String,
    message: String,
    metadata: String?,
    onDismiss: () -> Unit,
    onOpenApp: () -> Unit,
    onSnooze: () -> Unit,
    onMute: () -> Unit
) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Card(modifier = Modifier.padding(16.dp)) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(text = "Сигнал: $type", style = MaterialTheme.typography.titleLarge)
                    Text(text = message)
                    if (!metadata.isNullOrEmpty()) Text(text = metadata)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = onSnooze) { Text("Отложить 5 мин") }
                        Spacer(Modifier.width(8.dp))
                        TextButton(onClick = onMute) { Text("Отключить звук") }
                        Spacer(Modifier.width(8.dp))
                        TextButton(onClick = onOpenApp) { Text("Открыть приложение") }
                    }
                }
            }
        }
    }
}

 */
