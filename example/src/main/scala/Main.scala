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
import com.jsuereth.gl.math._
import com.jsuereth.gl.io.{
    withMemoryStack,
    VertexArrayObject,
    ShaderLoadingEnvironment,
    ActiveTextures
}
import org.lwjgl.system.MemoryStack
import com.jsuereth.gl.mesh.parser.ObjFileParser
import com.jsuereth.gl.scene._
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


    def run(): Unit = {
        System.out.println("Hello LWJGL " + Version.getVersion() + "!");
        System.out.println("Example shaders")
        System.out.println("--- Vertex Shader ---")
        System.out.println(CartoonShader.vertexShaderCode)
        System.out.println("--- Fragment Shader ---")
        System.out.println(CartoonShader.fragmentShaderCode)
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
        val models = ObjFileParser.parse(getClass.getClassLoader.getResourceAsStream("mesh/deep_space_1_11.obj"))
        System.out.println("Done loading models!")
        for ((name, mesh) <- models) {
            System.err.println(s" - Loaded model [$name] w/ ${mesh.vertices.size} vertices, ${mesh.normals.size} normals, ${mesh.textureCoords.size} texcoords, ${mesh.faces.size} faces")
        }
        val mesh =
          models.iterator.next._2
        val uglyTexture = Texture.loadImage(getClass.getClassLoader.getResourceAsStream("mesh/texture/foil_silver_ramp.png"))
        
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
        val vao = withMemoryStack(mesh.loadVao)
        CartoonShader.load()

        System.out.println("-- Shader struct debug --")
        System.out.println(s" world: ${CartoonShader.debugUniform("world")}")
        System.out.println(s" world.light: ${CartoonShader.debugUniform("world.light")}")
        System.out.println(s" world.eye: ${CartoonShader.debugUniform("world.eye")}")
        System.out.println(s" world.view: ${CartoonShader.debugUniform("world.view")}")
        System.out.println(s" world.projection: ${CartoonShader.debugUniform("world.projection")}")

        // Render a scene using cartoon shader.
        def render(): Unit = {
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT) // clear the framebuffer
            glEnable(GL_DEPTH_TEST)
            glEnable(GL_CULL_FACE)
            glCullFace(GL_BACK)
            glEnable(GL_TEXTURE)
            glEnable(GL_TEXTURE_2D)

            CartoonShader.bind()
            withMemoryStack {
                delegate env for ShaderLoadingEnvironment {
                    val stack = the[MemoryStack]
                    val textures = ActiveTextures()
                }
                CartoonShader.world := WorldData(light = scene.lights.next,
                                                 eye = scene.camera.eyePosition,
                                                 view = scene.camera.viewMatrix,
                                                 projection = projectionMatrix)
                CartoonShader.materialKdTexture := uglyTexture
                for (o <- scene.objectsInRenderOrder) {
                    env.push()
                    // TODO - pull material from objects.
                    CartoonShader.materialShininess := 1.3f
                    CartoonShader.materialKd := 0.5f
                    CartoonShader.materialKs := 0.4f
                    CartoonShader.modelMatrix := o.modelMatrix
                    // TODO - pull the VAO for the model.
                    vao.draw()
                    env.pop()
                }
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