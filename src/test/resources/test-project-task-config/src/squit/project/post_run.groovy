import java.time.Instant
Thread.sleep(1)
def file = new File(config.getString("rootDir") + "/build/post_run.txt")
file.text = Instant.now().toEpochMilli().toString()
Thread.sleep(1)
