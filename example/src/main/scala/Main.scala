/*
 * Copyright 2019 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.lwjgl.Version
import org.lwjgl.glfw.Callbacks._
import org.lwjgl.opengl._
import org.lwjgl.glfw._
import org.lwjgl.glfw.GLFW._
import org.lwjgl.opengl.GL
import org.lwjgl.glfw.GLFW._
import org.lwjgl.opengl.GL11._
import org.lwjgl.system.MemoryUtil._
import com.jsuereth.gl.math.{given _, _}
import com.jsuereth.gl.io.{
    withMemoryStack,
    VertexArrayObject,
    ShaderLoadingEnvironment,
    ActiveTextures
}
import org.lwjgl.system.MemoryStack
import com.jsuereth.gl.mesh.parser.ObjFileParser
import com.jsuereth.gl.scene._
import com.jsuereth.gl.mesh._
import com.jsuereth.gl.texture.{
    Texture,
    Texture2D
}
import com.jsuereth.gl.math._

object Main {
    val NULL=0L
    val WIDTH = 1920
    val HEIGHT = 1080
    var window: Long = 0
    var scene: Scene = null

    val meshLoader = MeshLoader(ClassloaderResourceLookup(Main.getClass.getClassLoader))
    /** A quick hacky texture manager that just loads + remembers by file name. */
    object TextureManager {
        private val textures = collection.mutable.Map.empty[String, Texture2D]

        private def load(location: String): Texture2D = {
            val in = Main.getClass.getClassLoader.getResourceAsStream(location)
            if (in == null) throw new RuntimeException(s"Unable to load texture @ $location")
            try Texture.loadImage(in)
            finally in.close()
        }

        def get(location: String): Texture2D =
          textures.get(location) match {
              case Some(value) => value
              case None =>
                System.err.println(s"Loading texture: $location")
                val texture = load(location)
                textures.put(location, texture)
                texture
          }
    }


    def run(): Unit = {
        System.out.println("Hello LWJGL " + Version.getVersion() + "!");
        System.out.println("Example shaders")
        System.out.println("--- Vertex Shader ---")
        System.out.println(SimpleLightShader.vertexShaderCode)
        System.out.println("--- Fragment Shader ---")
        System.out.println(SimpleLightShader.fragmentShaderCode)
        System.out.println("---  ---")
        try {
            init()
            loop()

            // Release window and window callbacks
            glfwFreeCallbacks(window)
            glfwDestroyWindow(window)
        } finally {
            // Terminate GLFW and release the GLFWerrorfun
            glfwTerminate()
            glfwSetErrorCallback(null).free()
        }
    }

    private def init(): Unit = {
        // Setup an error callback. The default implementation
        // will print the error message in System.err.
        GLFWErrorCallback.createPrint(System.err).set()

        // Initialize GLFW. Most GLFW functions will not work before doing this.
        if (!glfwInit()) {
            throw new IllegalStateException("Unable to initialize GLFW");
        }

        // Configure our window
        glfwDefaultWindowHints() // optional, the current window hints are already the default
        glfwWindowHint(GLFW_VISIBLE, GL_FALSE) // the window will stay hidden after creation
        glfwWindowHint(GLFW_RESIZABLE, GL_TRUE) // the window will be resizable

        

        // Create the window
        window = glfwCreateWindow(WIDTH, HEIGHT, "Hello World!", NULL, NULL)
        if (window == NULL) {
            throw new RuntimeException("Failed to create the GLFW window")
        }
        val cameraAmount = 0.5f
        // Setup a key callback. It will be called every time a key is pressed, repeated or released.
        glfwSetKeyCallback(window, (window, key, scancode, action, mods) => {
            if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE) {
                glfwSetWindowShouldClose(window, true) // We will detect this in the rendering loop
            } else if (key == GLFW_KEY_W && action == GLFW_PRESS) {
                scene.camera.moveForward(cameraAmount)
            } else if (key == GLFW_KEY_S && action == GLFW_PRESS) {
                scene.camera.moveForward(-cameraAmount)
            } else if (key == GLFW_KEY_A && action == GLFW_PRESS) {
                scene.camera.moveRight(-cameraAmount)
            } else if (key == GLFW_KEY_D && action == GLFW_PRESS) {
                scene.camera.moveRight(cameraAmount)
            } else if (key == GLFW_KEY_Z && action == GLFW_PRESS) {
                scene.camera.moveUp(-cameraAmount)
            } else if (key == GLFW_KEY_X && action == GLFW_PRESS) {
                scene.camera.moveUp(cameraAmount)
            } else if (key == GLFW_KEY_UP && action == GLFW_PRESS) {
                scene.camera.turnUp(cameraAmount)
            } else if (key == GLFW_KEY_DOWN && action == GLFW_PRESS) {
                scene.camera.turnUp(-cameraAmount)
            } else if (key == GLFW_KEY_LEFT && action == GLFW_PRESS) {
                scene.camera.turnRight(-cameraAmount)
            } else if (key == GLFW_KEY_RIGHT && action == GLFW_PRESS) {
                scene.camera.turnRight(cameraAmount)
            }
        });

        // Get the resolution of the primary monitor
        val vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor())
        // Center our window
        glfwSetWindowPos(
                window,
                (vidmode.width() - WIDTH) / 2,
                (vidmode.height() - HEIGHT) / 2
        )

        // Make the OpenGL context current
        glfwMakeContextCurrent(window)
        // Enable v-sync
        glfwSwapInterval(1)

        // Make the window visible
        glfwShowWindow(window)
    }

    private def loop(): Unit = {
        // This line is critical for LWJGL's interoperation with GLFW's
        // OpenGL context, or any context that is managed externally.
        // LWJGL detects the context that is current in the current thread,
        // creates the ContextCapabilities instance and makes the OpenGL
        // bindings available for use.
        GL.createCapabilities()

        // Set the clear color
        glClearColor(0.1f, 0.1f, 0.1f, 0.1f)
        glViewport(0,0,WIDTH, HEIGHT)
        glEnable(GL_TEXTURE)


        // Load models and shader.
        val models = meshLoader.loadObjects("mesh/deep_space_1_11.obj")
        System.out.println("Done loading models!")
        for ((name, mesh) <- models) {
            System.err.println(s" - Loaded model [$name]: $mesh")
        }
        // Preload textures.
        val mesh = models.iterator.next._2 
        val scaleFactor = 1f  
        // TODO - start rendering using the scene...
        scene = SimpleStaticSceneBuilder().
          add(mesh).scale(scaleFactor,scaleFactor,scaleFactor).orientation(Quaternion.fromEuler(0f,90f,90f)).done().
          add(mesh).scale(scaleFactor,scaleFactor,scaleFactor).pos(Vec3(5f,2f,4f)).orientation(Quaternion.fromEuler(90f,0f,0f)).done().
          add(mesh).scale(scaleFactor,scaleFactor,scaleFactor).pos(Vec3(-5f,4f,4f)).orientation(Quaternion.fromEuler(0f,0f,90f)).done().
          light(Vec3(20f,100f,-5f)).
          done()
        val projectionMatrix = 
          Matrix4.perspective(45f, WIDTH.toFloat/HEIGHT.toFloat, 1f, 200f) 
        val loadedMesh = withMemoryStack(load(mesh))
        SimpleLightShader.load()

        System.out.println("-- Shader struct debug --")
        System.out.println(s" world: ${SimpleLightShader.debugUniform("world")}")
        System.out.println(s" world.light: ${SimpleLightShader.debugUniform("world.light")}")
        System.out.println(s" world.eye: ${SimpleLightShader.debugUniform("world.eye")}")
        System.out.println(s" world.view: ${SimpleLightShader.debugUniform("world.view")}")
        System.out.println(s" world.projection: ${SimpleLightShader.debugUniform("world.projection")}")


        def meshRenderCtx(using ShaderLoadingEnvironment): MeshRenderContext =
            new MeshRenderContext {
                // This is evil...
                def applyMaterial(material: RawMaterial): Unit = {
                    summon[ShaderLoadingEnvironment].pop()
                    summon[ShaderLoadingEnvironment].push()
                    SimpleLightShader.materialShininess := material.base.ns
                    SimpleLightShader.materialKd := material.base.kd
                    SimpleLightShader.materialKs := material.base.ks
                    material.textures.kd match {
                        case Some(ref) =>
                             SimpleLightShader.materialKdTexture := 
                                TextureManager.get(ref.filename)
                        case None => ()
                    }
                }
            }

        // Render a scene using cartoon shader.
        def render(): Unit = {
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT) // clear the framebuffer
            glEnable(GL_DEPTH_TEST)
            glEnable(GL_CULL_FACE)
            glCullFace(GL_BACK)
            glEnable(GL_TEXTURE)
            glEnable(GL_TEXTURE_2D)
            SimpleLightShader.bind()
            withMemoryStack {
                given env as ShaderLoadingEnvironment {
                    val stack = summon[MemoryStack]
                    val textures = ActiveTextures()
                }
                env.push()
                SimpleLightShader.world := WorldData(light = scene.lights.next,
                                                     eye = scene.camera.eyePosition,
                                                     view = scene.camera.viewMatrix,
                                                     projection = projectionMatrix)
                val ctx = meshRenderCtx
                for (o <- scene.objectsInRenderOrder) {
                    env.push()
                    SimpleLightShader.modelMatrix := o.modelMatrix
                    // TODO - pull the VAO for the model.
                    loadedMesh.render(ctx)
                    env.pop()
                }
                env.pop()
            }
        }
        
        

        // Run the rendering loop until the user has attempted to close
        // the window or has pressed the ESCAPE key.
        while (!glfwWindowShouldClose(window)) {
            render()
            glfwSwapBuffers(window)
            // Poll for window events. The key callback above will only be
            // invoked during this call.
            glfwPollEvents()
        }
    }

    def main(args: Array[String]): Unit = {
      run()
    }
}