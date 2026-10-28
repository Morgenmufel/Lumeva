import org.apache.avro.Schema
import org.apache.avro.compiler.specific.SpecificCompiler
import org.apache.avro.generic.GenericData

plugins {
    `java-library`
}

java {
    toolchain { languageVersion = JavaLanguageVersion.of(25) }
}

dependencies {
    api("org.apache.avro:avro:1.12.2")
}

@CacheableTask
abstract class GenerateAvroJavaTask : DefaultTask() {

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val schemaFiles: ConfigurableFileCollection

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun generate() {
        val out = outputDir.get().asFile
        out.deleteRecursively()
        out.mkdirs()

        val parser = Schema.Parser()
        val ordered = schemaFiles.files.sortedWith(
            compareBy<File>({ !it.parentFile.name.equals("types") }, { it.invariantSeparatorsPath })
        )
        ordered.map(parser::parse).forEach { schema ->
            SpecificCompiler(schema).apply {
                setStringType(GenericData.StringType.String)
                setFieldVisibility(SpecificCompiler.FieldVisibility.PRIVATE)
                setCreateSetters(false)
                setOutputCharacterEncoding("UTF-8")
            }.compileToDestination(null, out)
        }
    }
}

val generateAvroJava by tasks.registering(GenerateAvroJavaTask::class) {
    schemaFiles.from(fileTree("src/main/avro") { include("**/*.avsc") })
    outputDir = layout.buildDirectory.dir("generated/sources/avro/main")
}

sourceSets.main { java.srcDir(generateAvroJava) }