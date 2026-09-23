package com.philia093.neofactory.render;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.utils.Disposable;

/**
 * The shader every block of the world is drawn with.
 * <p>
 * A corner carries its position in world coordinates, the spot of the picture it shows, the layer that
 * picture lives in inside the texture array and the colour the fixed shading and the shadow of its
 * neighbours left of the tint of the block. The program therefore needs no model matrix: a mesh is drawn
 * where its corners already are.
 * <p>
 * The distance of a corner from the eye fades it into the colour of the sky, so the world ends in the
 * sky instead of at a hard edge, and a texel that is nearly transparent is thrown away, which is how a
 * canopy of leaves keeps the holes between its leaves.
 * <p>
 * The program is written for the OpenGL 3.2 core context the launcher asks for: an attribute is
 * declared with {@code in}, a varying with {@code out}, and the sampler is an array of textures.
 */
public class BlockShader implements Disposable {

    /**
     * Layout of one corner: position, texture coordinate, layer of the picture and colour.
     * <p>
     * The names are the ones this shader declares, which is how a mesh and the program find each other.
     */
    public static final VertexAttributes ATTRIBUTES = new VertexAttributes(
            VertexAttribute.Position(),
            new VertexAttribute(VertexAttributes.Usage.TextureCoordinates, 2, "a_texCoords"),
            new VertexAttribute(VertexAttributes.Usage.Generic, 1, "a_layer"),
            VertexAttribute.ColorUnpacked());

    private static final String VERTEX_SHADER = """
            #version 150
            in vec3 a_position;
            in vec2 a_texCoords;
            in float a_layer;
            in vec4 a_color;

            uniform mat4 u_projViewTrans;
            uniform vec3 u_cameraPosition;
            uniform float u_fogStart;
            uniform float u_fogEnd;

            out vec2 v_texCoords;
            out float v_layer;
            out vec4 v_color;
            out float v_fog;

            void main() {
                v_texCoords = a_texCoords;
                v_layer = a_layer;
                v_color = a_color;
                float distance = length(a_position - u_cameraPosition);
                v_fog = clamp((distance - u_fogStart) / max(u_fogEnd - u_fogStart, 1.0), 0.0, 1.0);
                gl_Position = u_projViewTrans * vec4(a_position, 1.0);
            }
            """;

    private static final String FRAGMENT_SHADER = """
            #version 150
            in vec2 v_texCoords;
            in float v_layer;
            in vec4 v_color;
            in float v_fog;

            uniform sampler2DArray u_texture;
            uniform vec3 u_fogColor;

            out vec4 fragColor;

            void main() {
                vec4 texel = texture(u_texture, vec3(v_texCoords, v_layer));
                if (texel.a < 0.1) {
                    discard;
                }
                fragColor = vec4(mix(texel.rgb * v_color.rgb, u_fogColor, v_fog), texel.a * v_color.a);
            }
            """;

    private final ShaderProgram program = new ShaderProgram(VERTEX_SHADER, FRAGMENT_SHADER);

    /** Texture unit the array of pictures is bound to. */
    private static final int TEXTURE_UNIT = 0;

    /** Creates the program and reports a shader that could not be built. */
    public BlockShader() {
        if (!program.isCompiled()) {
            throw new IllegalStateException("The block shader did not compile: " + program.getLog());
        }
    }

    /**
     * Makes this program the one the next meshes are drawn with.
     *
     * @param camera camera the world is seen through
     * @param pictures pictures of the world, bound to the sampler
     * @param fogColor colour the distance fades into, usually the sky
     * @param fogStart distance the fog starts at, in blocks
     * @param fogEnd distance everything is fog by, in blocks
     */
    public void begin(Camera camera, BlockPictures pictures, Color fogColor, float fogStart,
            float fogEnd) {
        program.bind();
        program.setUniformMatrix("u_projViewTrans", camera.combined);
        program.setUniformf("u_cameraPosition", camera.position);
        program.setUniformf("u_fogColor", fogColor.r, fogColor.g, fogColor.b);
        program.setUniformf("u_fogStart", fogStart);
        program.setUniformf("u_fogEnd", fogEnd);
        program.setUniformi("u_texture", TEXTURE_UNIT);
        pictures.bind(TEXTURE_UNIT);
    }

    /**
     * Draws one mesh of the world.
     *
     * @param mesh mesh to draw
     */
    public void render(Mesh mesh) {
        mesh.render(program, GL20.GL_TRIANGLES);
    }

    /** Ends the pass, so the next one starts from a shader it binds itself. */
    public void end() {
        // Nothing to unbind: every pass binds the program it needs.
    }

    @Override
    public void dispose() {
        program.dispose();
    }

    @Override
    public String toString() {
        return "BlockShader(" + program.getHandle() + ")";
    }
}
