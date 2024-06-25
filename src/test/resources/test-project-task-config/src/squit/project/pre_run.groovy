import java.time.Instant
Thread.sleep(10)
def file = new File(config.getString("rootDir") + "/build/pre_run.txt")
file.text = Instant.now().toEpochMilli().toString()
Thread.sleep(10)
