import os
import pathlib
import subprocess
import textwrap

stub = pathlib.Path(os.environ["TEMP"]) / "scheduler-core-stub"

classes = {
    "cn/edu/whu/zhuyuhan/scheduler/common/util/cron/CronUtils.java": textwrap.dedent(
        """\
        package cn.edu.whu.zhuyuhan.scheduler.common.util.cron;

        public class CronUtils {
            public static boolean valid(String cron) {
                return cron != null && cron.contains(" ");
            }
        }
        """
    ),
    "cn/edu/whu/zhuyuhan/scheduler/task/ScheduleTaskRegistrar.java": textwrap.dedent(
        """\
        package cn.edu.whu.zhuyuhan.scheduler.task;

        import cn.edu.whu.zhuyuhan.scheduler.task.model.ScheduleOpen;

        public class ScheduleTaskRegistrar {
            public static void register(ScheduleOpen scheduleOpen) {
            }

            public void destroy() {
            }
        }
        """
    ),
    "cn/edu/whu/zhuyuhan/scheduler/task/model/ScheduleRegisterTask.java": textwrap.dedent(
        """\
        package cn.edu.whu.zhuyuhan.scheduler.task.model;

        public class ScheduleRegisterTask {
        }
        """
    ),
    "cn/edu/whu/zhuyuhan/scheduler/task/model/ScheduleCommonTask.java": textwrap.dedent(
        """\
        package cn.edu.whu.zhuyuhan.scheduler.task.model;

        public class ScheduleCommonTask extends ScheduleRegisterTask {
            public ScheduleCommonTask(String taskName, String cron, Runnable runnable,
                                      Object context, String id, Object extra,
                                      Boolean async, Boolean distributed, Boolean special) {
            }

            public ScheduleCommonTask(String taskName, long delay, Runnable runnable,
                                      Object context, String id, Object extra,
                                      Boolean async, Boolean distributed, Boolean special) {
            }
        }
        """
    ),
    "cn/edu/whu/zhuyuhan/scheduler/task/model/ScheduleOpen.java": textwrap.dedent(
        """\
        package cn.edu.whu.zhuyuhan.scheduler.task.model;

        public class ScheduleOpen {
            public ScheduleOpen(ScheduleRegisterTask task) {
            }
        }
        """
    ),
}

for rel, content in classes.items():
    path = stub / "src/main/java" / rel
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(content, encoding="utf-8")

(stub / "pom.xml").write_text(
    """<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>
  <groupId>cn.edu.whu.zhuyuhan</groupId>
  <artifactId>scheduler-core</artifactId>
  <version>0.1.2</version>
  <build>
    <plugins>
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-compiler-plugin</artifactId>
        <version>3.8.0</version>
        <configuration>
          <source>1.8</source>
          <target>1.8</target>
        </configuration>
      </plugin>
    </plugins>
  </build>
</project>
""",
    encoding="utf-8",
)

mvn = "mvn.cmd" if os.name == "nt" else "mvn"
r = subprocess.run([mvn, "-q", "install"], cwd=str(stub), shell=(os.name == "nt"))
raise SystemExit(r.returncode)
