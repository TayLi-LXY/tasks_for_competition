import os
import pathlib
import subprocess
import textwrap

stub = pathlib.Path(os.environ["TEMP"]) / "easyfile-stub"
pkg = stub / "src/main/java/cc/eamon/easyfile"
pkg.mkdir(parents=True, exist_ok=True)

(pkg / "FileTools.java").write_text(
    textwrap.dedent(
        """\
        package cc.eamon.easyfile;

        import java.io.BufferedReader;
        import java.io.File;
        import java.io.FileInputStream;
        import java.io.InputStream;
        import java.io.InputStreamReader;
        import java.nio.charset.StandardCharsets;
        import java.util.ArrayList;
        import java.util.List;

        public class FileTools {
            public static List<File> readFiles(String path) {
                List<File> files = new ArrayList<File>();
                File root = new File(path);
                if (!root.exists()) {
                    return files;
                }
                collect(root, files);
                return files;
            }

            private static void collect(File file, List<File> files) {
                if (file.isFile()) {
                    files.add(file);
                    return;
                }
                File[] children = file.listFiles();
                if (children == null) {
                    return;
                }
                for (File child : children) {
                    collect(child, files);
                }
            }

            public static String loadFile(File file) {
                try {
                    return inputStream2Str(new FileInputStream(file));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            public static String inputStream2Str(InputStream inputStream) {
                try {
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(inputStream, StandardCharsets.UTF_8));
                    StringBuilder builder = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        builder.append(line).append('\\n');
                    }
                    reader.close();
                    return builder.toString();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
        """
    ),
    encoding="utf-8",
)

(stub / "pom.xml").write_text(
    """<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>
  <groupId>cc.eamon</groupId>
  <artifactId>easyfile</artifactId>
  <version>0.0.3</version>
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
