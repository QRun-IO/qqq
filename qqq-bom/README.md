# QQQ BOM

Bill of Materials for QQQ dependencies. Import this to manage QQQ module versions.

## Usage

```xml
<dependencyManagement>
   <dependencies>
      <dependency>
         <groupId>com.kingsrook.qqq</groupId>
         <artifactId>qqq-bom</artifactId>
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

GNU Affero General Public License v3.0
