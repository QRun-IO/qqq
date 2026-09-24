# QQQ BOM

Bill of Materials for QQQ dependencies. Import this to manage QQQ module versions.

Set `qqq.version` to the published QQQ release you consume. See the [current release status](../README.md) before selecting a 4.0 version. The BOM manages QQQ libraries; it does not add them to your application.

## Usage

```xml
<dependencyManagement>
   <dependencies>
      <dependency>
         <groupId>com.kingsrook.qqq</groupId>
         <artifactId>qqq-bom-pom</artifactId>
         <version>${qqq.version}</version>
         <type>pom</type>
         <scope>import</scope>
      </dependency>
   </dependencies>
</dependencyManagement>

<dependencies>
   <!-- No version needed - managed by BOM -->
   <dependency>
      <groupId>com.kingsrook.qqq</groupId>
      <artifactId>qqq-backend-core</artifactId>
   </dependency>
</dependencies>
```

## License

See the repository [LICENSE](../LICENSE), [NOTICE](../NOTICE), and the license headers in individual source files.
