package com.philia093.neofactory.render;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.Disposable;
import com.philia093.neofactory.block.Block;

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
     * Layout of one corner: position, texture coordinate, layer of the picture, colour and frames.
     * <p>
     * The names are the ones this shader declares, which is how a mesh and the program find each other.
     * The amount of frames is the last number of a corner because it was added after the rest, see
     * {@link MeshData#FRAMES}.
     */
    public static final VertexAttributes ATTRIBUTES = new VertexAttributes(
            VertexAttribute.Position(),
            new VertexAttribute(VertexAttributes.Usage.TextureCoordinates, 2, "a_texCoords"),
            new VertexAttribute(VertexAttributes.Usage.Generic, 1, "a_layer"),
            VertexAttribute.ColorUnpacked(),
            new VertexAttribute(VertexAttributes.Usage.Generic, 1, "a_frames"));

    private static final String VERTEX_SHADER = """
            #version 150
            in vec3 a_position;
            in vec2 a_texCoords;
            in float a_layer;
            in vec4 a_color;
            in float a_frames;

            uniform mat4 u_projViewTrans;
            uniform mat4 u_modelTrans;
            uniform vec3 u_cameraPosition;
            uniform float u_fogStart;
            uniform float u_fogEnd;
            uniform float u_frame;

            out vec2 v_texCoords;
            out float v_layer;
            out vec4 v_color;
            out float v_fog;

            void main() {
                v_texCoords = a_texCoords;
                // A picture that is a strip of frames lives in as many layers as it has frames, one
                // below the other, so the frame of this corner is the layer of the first frame plus
                // the frame the world is in, wrapped around the run. A still picture names one frame
                // and therefore always lands on the layer it was meshed with.
                v_layer = a_layer + mod(u_frame, max(a_frames, 1.0));
                v_color = a_color;
                // A mesh of a section already stands where it belongs, so its model matrix is the one
                // that changes nothing; a mesh of an item or a body is moved and scaled by this one.
                vec4 world = u_modelTrans * vec4(a_position, 1.0);
                float distance = length(world.xyz - u_cameraPosition);
                v_fog = clamp((distance - u_fogStart) / max(u_fogEnd - u_fogStart, 1.0), 0.0, 1.0);
                gl_Position = u_projViewTrans * vec4(world.xyz, 1.0);
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

    /** Frame of an animated picture the next pass shows, see {@link #animationFrame(int)}. */
    private float animationFrame;

    /** Model matrix that moves nothing, the one a mesh of a section is drawn with. */
    private static final Matrix4 NO_MOVE = new Matrix4();

    /** Texture unit the array of pictures is bound to. */
    private static final int TEXTURE_UNIT = 0;

    /** Creates the program and reports a shader that could not be built. */
    public BlockShader() {
        if (!program.isCompiled()) {
            throw new IllegalStateException("The block shader did not compile: " + program.getLog());
        }
    }

    /**
     * Frame of an animated picture the next pass shows.
     * <p>
     * Every picture of the world shares the frame, which is what makes a lake of water one body and
     * not a field of pictures that each started at their own moment; a still picture names one frame
     * and is not moved by it at all. The renderer sets it once a frame out of the tick the world is
     * in, see {@link BlockAnimation#frameIndex(long, com.philia093.neofactory.block.Block.Animation)}
     * and {@link #ANIMATION}, and the meshes that were built before the frame was ever set stand
     * still on their first frame.
     *
     * @param frame frame of the animation that is running, counted from zero
     */
    public void animationFrame(int frame) {
        this.animationFrame = Math.max(0, frame);
    }

    /**
     * The round an animated picture of the world runs through, four ticks a frame.
     * <p>
     * One round serves every picture: a strip of four frames passes through them four times as fast
     * as one of sixty four, because the shader wraps the frame around the run of its own picture, see
     * {@link MeshData#FRAMES}. A machine whose gear turns four steps a second is therefore one strip
     * of four frames on disk and nothing else.
     */
    public static final Block.Animation ANIMATION = new Block.Animation(64, 4);

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
        program.setUniformMatrix("u_modelTrans", NO_MOVE);
        program.setUniformf("u_cameraPosition", camera.position);
        program.setUniformf("u_fogColor", fogColor.r, fogColor.g, fogColor.b);
        program.setUniformf("u_fogStart", fogStart);
        program.setUniformf("u_fogEnd", fogEnd);
        program.setUniformi("u_texture", TEXTURE_UNIT);
        program.setUniformf("u_frame", animationFrame);
        pictures.bind(TEXTURE_UNIT);
    }

    /**
     * Draws one mesh of the world.
     *
     * @param mesh mesh to draw
     */
    public void render(Mesh mesh) {
        render(mesh, NO_MOVE);
    }

    /**
     * Draws one mesh at a place and size of its own.
     * <p>
     * A mesh of a section already stands where it belongs and is drawn with the matrix that moves
     * nothing; this is what a mesh of an item or of a body is drawn with, because the same little cube
     * is used for every item of a kind and only its place in the world differs.
     *
     * @param mesh mesh to draw
     * @param model matrix placing it in the world
     */
    public void render(Mesh mesh, Matrix4 model) {
        program.setUniformMatrix("u_modelTrans", model);
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
