import os
import pathlib
import subprocess
import textwrap

stub = pathlib.Path(os.environ["TEMP"]) / "auto-config-stub"
pkg = stub / "src/main/java/cc/eamon/open/auto/config/code/detail"
pkg.mkdir(parents=True, exist_ok=True)

(pkg / "ModelConfig.java").write_text(
    textwrap.dedent(
        """\
        package cc.eamon.open.auto.config.code.detail;

        import java.io.Serializable;
        import java.util.ArrayList;
        import java.util.List;

        public class ModelConfig implements Serializable {
            private static final long serialVersionUID = 1L;
            private String id;
            private String name;
            private String type;
            private String reference;
            private List<Object> fields = new ArrayList<Object>();
            public String getId() { return id; }
            public void setId(String id) { this.id = id; }
            public String getName() { return name; }
            public void setName(String name) { this.name = name; }
            public String getType() { return type; }
            public void setType(String type) { this.type = type; }
            public String getReference() { return reference; }
            public void setReference(String reference) { this.reference = reference; }
            public List<Object> getFields() { return fields; }
            public void setFields(List<Object> fields) { this.fields = fields; }
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
  <groupId>cc.eamon.open</groupId>
  <artifactId>auto-config</artifactId>
  <version>1.0.6</version>
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
