package com.arwallcanvas

import android.content.Context
import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import com.google.ar.core.Session
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * Renderizador OpenGL que projeta o desenho na superfície AR
 * e gerencia a visualização estéreo para VR (Cardboard).
 */
class ARWallRenderer(
    private val session: Session,
    private val context: Context
) : GLSurfaceView.Renderer {

    private val viewMatrix = FloatArray(16)
    private val projMatrix = FloatArray(16)
    private val modelMatrix = FloatArray(16)

    // Matriz para VR (olho esquerdo/direito)
    private val leftEyeMatrix = FloatArray(16)
    private val rightEyeMatrix = FloatArray(16)

    private var isVRMode = false
    private var program = 0

    // Coordenadas do plano da parede (quadrado texturizado)
    private val wallVertices = floatArrayOf(
        // Posições (x, y, z)   // UV
        -0.5f, -0.5f, 0f,       0f, 0f,
         0.5f, -0.5f, 0f,       1f, 0f,
         0.5f,  0.5f, 0f,       1f, 1f,
        -0.5f,  0.5f, 0f,       0f, 1f
    )

    private val wallIndices = intArrayOf(
        0, 1, 2,
        0, 2, 3
    )

    private var vbo = IntArray(1)
    private var ebo = IntArray(1)
    private var vao = IntArray(1)

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES30.glClearColor(0f, 0f, 0f, 0f)
        GLES30.glEnable(GLES30.GL_DEPTH_TEST)
        GLES30.glEnable(GLES30.GL_BLEND)
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA)

        // Compilar shaders
        program = createProgram(
            VERTEX_SHADER_CODE,
            FRAGMENT_SHADER_CODE
        )

        // Configurar geometria do plano
        setupWallGeometry()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES30.glViewport(0, 0, width, height)

        // Atualizar projeção com base no aspect ratio
        val aspect = width.toFloat() / height.toFloat()
        Matrix.perspectiveM(projMatrix, 0, 45f, aspect, 0.1f, 100f)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT)

        if (isVRMode) {
            // Renderizar olho esquerdo
            GLES30.glViewport(0, 0, 400, 800) // metade esquerda
            drawForEye(leftEyeMatrix)

            // Renderizar olho direito
            GLES30.glViewport(400, 0, 400, 800) // metade direita
            drawForEye(rightEyeMatrix)
        } else {
            GLES30.glViewport(0, 0, 800, 800) // tela cheia
            drawForEye(viewMatrix)
        }
    }

    private fun drawForEye(eyeMatrix: FloatArray) {
        GLES30.glUseProgram(program)

        // Matriz MVP combinada
        val mvpMatrix = FloatArray(16)
        Matrix.multiplyMM(mvpMatrix, 0, projMatrix, 0, eyeMatrix, 0)
        Matrix.multiplyMM(mvpMatrix, 0, mvpMatrix, 0, modelMatrix, 0)

        val mvpHandle = GLES30.glGetUniformLocation(program, "uMVPMatrix")
        GLES30.glUniformMatrix4fv(mvpHandle, 1, false, mvpMatrix, 0)

        // Desenhar o plano da parede
        GLES30.glBindVertexArray(vao[0])
        GLES30.glDrawElements(GLES30.GL_TRIANGLES, wallIndices.size, GLES30.GL_UNSIGNED_INT, 0)
        GLES30.glBindVertexArray(0)
    }

    private fun setupWallGeometry() {
        GLES30.glGenVertexArrays(1, vao, 0)
        GLES30.glGenBuffers(1, vbo, 0)
        GLES30.glGenBuffers(1, ebo, 0)

        GLES30.glBindVertexArray(vao[0])

        // VBO
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vbo[0])
        val vertexBuffer = java.nio.ByteBuffer
            .allocateDirect(wallVertices.size * 4)
            .order(java.nio.ByteOrder.nativeOrder())
            .asFloatBuffer()
        vertexBuffer.put(wallVertices)
        vertexBuffer.position(0)
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, wallVertices.size * 4, vertexBuffer, GLES30.GL_STATIC_DRAW)

        // Posição (3 floats)
        GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, 5 * 4, 0)
        GLES30.glEnableVertexAttribArray(0)

        // UV (2 floats)
        GLES30.glVertexAttribPointer(1, 2, GLES30.GL_FLOAT, false, 5 * 4, 3 * 4)
        GLES30.glEnableVertexAttribArray(1)

        // EBO
        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, ebo[0])
        val indexBuffer = java.nio.ByteBuffer
            .allocateDirect(wallIndices.size * 4)
            .order(java.nio.ByteOrder.nativeOrder())
            .asIntBuffer()
        indexBuffer.put(wallIndices)
        indexBuffer.position(0)
        GLES30.glBufferData(GLES30.GL_ELEMENT_ARRAY_BUFFER, wallIndices.size * 4, indexBuffer, GLES30.GL_STATIC_DRAW)

        GLES30.glBindVertexArray(0)
    }

    fun clearOverlay() {
        // Limpar textura do desenho
    }

    fun setVRMode(enabled: Boolean) {
        isVRMode = enabled
        if (enabled) {
            // Configurar matrizes para estereoscopia
            val eyeOffset = 0.032f // 32mm entre olhos
            Matrix.setIdentityM(leftEyeMatrix, 0)
            Matrix.translateM(leftEyeMatrix, 0, -eyeOffset, 0f, 0f)
            Matrix.setIdentityM(rightEyeMatrix, 0)
            Matrix.translateM(rightEyeMatrix, 0, eyeOffset, 0f, 0f)
        }
    }

    private fun createProgram(vertexSource: String, fragmentSource: String): Int {
        val vertexShader = compileShader(GLES30.GL_VERTEX_SHADER, vertexSource)
        val fragmentShader = compileShader(GLES30.GL_FRAGMENT_SHADER, fragmentSource)

        val program = GLES30.glCreateProgram()
        GLES30.glAttachShader(program, vertexShader)
        GLES30.glAttachShader(program, fragmentShader)
        GLES30.glLinkProgram(program)
        return program
    }

    private fun compileShader(type: Int, source: String): Int {
        val shader = GLES30.glCreateShader(type)
        GLES30.glShaderSource(shader, source)
        GLES30.glCompileShader(shader)
        return shader
    }

    companion object {
        private val VERTEX_SHADER_CODE = """
            #version 300 es
            in vec3 aPosition;
            in vec2 aTexCoord;
            uniform mat4 uMVPMatrix;
            out vec2 vTexCoord;
            void main() {
                gl_Position = uMVPMatrix * vec4(aPosition, 1.0);
                vTexCoord = aTexCoord;
            }
        """.trimIndent()

        private val FRAGMENT_SHADER_CODE = """
            #version 300 es
            precision mediump float;
            in vec2 vTexCoord;
            uniform sampler2D uTexture;
            out vec4 fragColor;
            void main() {
                vec4 texColor = texture(uTexture, vTexCoord);
                // Grid overlay para referência
                vec2 grid = abs(fract(vTexCoord * 20.0) - 0.5);
                float gridLine = min(grid.x, grid.y);
                float gridAlpha = smoothstep(0.48, 0.5, gridLine) * 0.1;
                fragColor = mix(texColor, vec4(1.0, 1.0, 1.0, gridAlpha), gridAlpha);
                if (fragColor.a < 0.01) discard;
            }
        """.trimIndent()
    }
}
