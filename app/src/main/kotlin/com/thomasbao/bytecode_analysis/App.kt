/*
 * This Kotlin source file was generated by the Gradle 'init' task.
 */
package com.thomasbao.bytecode_analysis

import org.apache.bcel.classfile.ClassParser
import org.apache.bcel.classfile.ConstantClass
import org.jgrapht.graph.DefaultDirectedGraph
import org.jgrapht.graph.DefaultEdge
import org.jgrapht.Graph
import org.jgrapht.alg.shortestpath.FloydWarshallShortestPaths
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.io.path.absolutePathString
import kotlin.io.path.relativeTo


val AIRBNB_PACKAGE = "com/airbnb"

fun getConstantPoolClassRefs(filepath: String): Set<String> {
  val classParser = ClassParser(filepath)
  val javaClass = classParser.parse()
  val constantPool = javaClass.constantPool
  return constantPool.map { it as? ConstantClass }.filterNotNull().map {
    constantPool.getConstantUtf8(it.nameIndex).bytes
  }.toSet()
}

class Klass(val classPath: String, val classRefs: Set<String>)

fun getKlassFromFile(classPath: String, classRefs: Set<String>): Klass {
    return Klass(
      classPath,
      classRefs.filter { it.startsWith(AIRBNB_PACKAGE) && it != classPath }.toSet()
    )
}

fun main(args: Array<String>) {
    val inputDirPath = args[0]

    //val outputDirPath = args[1]

    val g: Graph<String, DefaultEdge> = DefaultDirectedGraph<String, DefaultEdge>(DefaultEdge::class.java)
    Files.walk(
        Paths.get(inputDirPath + "/" + AIRBNB_PACKAGE)
    ).forEach {
      if (it.fileName.toString().endsWith(".class")) {
        val classPath = it.toAbsolutePath().relativeTo(Paths.get(inputDirPath)).toString().removeSuffix(".class")
        println("parsing ${classPath}")

        val classRefs = getConstantPoolClassRefs(it.absolutePathString())



        g.addVertex(classPath)
        classRefs.filter { it.startsWith(AIRBNB_PACKAGE) && it != classPath }.forEach {
          g.addVertex(it)
          g.addEdge(classPath, it)
        }
      }
    }

  val floydWarshall = FloydWarshallShortestPaths(g)
  println("Graph has ${g.vertexSet().size} nodes and ${g.edgeSet().size} edges")
  println("ShortestsPaths count: ${floydWarshall.shortestPathsCount})")
}

