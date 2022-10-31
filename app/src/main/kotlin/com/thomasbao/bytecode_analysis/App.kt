/*
 * This Kotlin source file was generated by the Gradle 'init' task.
 */
package com.thomasbao.bytecode_analysis

import com.google.common.graph.GraphBuilder
import com.google.common.graph.Graphs
import com.google.common.graph.MutableGraph
import com.google.common.graph.Graph as GuavaGraph
import org.apache.bcel.classfile.ClassParser
import org.apache.bcel.classfile.ConstantClass
import org.jgrapht.graph.DefaultDirectedGraph
import org.jgrapht.graph.DefaultEdge
import org.jgrapht.Graph
import org.jgrapht.alg.shortestpath.FloydWarshallShortestPaths
import org.jgrapht.traverse.BreadthFirstIterator
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import java.util.stream.Collectors
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

fun getAirbnbKlass(classPath: String, classRefs: Set<String>): Klass {
    return Klass(
      classPath,
      classRefs.filter { it.startsWith(AIRBNB_PACKAGE) && it != classPath }.toSet()
    )
}

fun main(args: Array<String>) {
    val inputDirPath = args[0]

    val sourcePackage = args[1]



    val files = mutableSetOf<Path>()
    Files.walk(
        Paths.get(inputDirPath + "/" + AIRBNB_PACKAGE)
    ).forEach {
      files.add(it)
    }

    val klasses = Collections.synchronizedSet(HashSet<Klass>())
    val count = AtomicInteger(0)
    files.parallelStream().forEach {
      if (it.fileName.toString().endsWith(".class")) {
        val classPath = it.toAbsolutePath().relativeTo(Paths.get(inputDirPath)).toString().removeSuffix(".class")
        val classRefs = getConstantPoolClassRefs(it.absolutePathString())
        val klass = getAirbnbKlass(classPath, classRefs)
        klasses.add(klass)

        if (count.incrementAndGet() % 1000 == 0) {
          println("processed $count classfiles so far")
        }

      }
    }

    println("Finished parsing ${klasses.size} classFiles")
    val gg: MutableGraph<String> = GraphBuilder.directed().build()

    klasses.forEach {
      val source = it.classPath
      gg.addNode(source)
      it.classRefs.forEach {
        gg.addNode(it)
        gg.putEdge(source, it)
      }
    }

  println("Graph has ${gg.nodes().size} nodes and ${gg.edges().size} edges")


  val startingNodes = gg.nodes().filter {
    it.startsWith(sourcePackage)
  }.toSet()

  val reachableNodes = startingNodes.map {
    Graphs.reachableNodes(gg, it)
  }.flatten().toSet()


  println("$sourcePackage has ${reachableNodes.size} reachable class files")





  //val floydWarshall = FloydWarshallShortestPaths(g)
  //println("ShortestsPaths count: ${floydWarshall.shortestPathsCount})")
}

