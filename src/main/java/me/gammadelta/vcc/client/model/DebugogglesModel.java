package me.gammadelta.vcc.client.model;

import net.minecraft.client.renderer.entity.model.BipedModel;

public class DebugogglesModel extends BipedModel {
    protected DebugogglesModel(float modelSize, float yOffsetIn, int textureWidthIn, int textureHeightIn) {
        super(modelSize, yOffsetIn, textureWidthIn, textureHeightIn);

        this.bipedHead.showModel = false;
        this.bipedBody.showModel = false;
        this.bipedLeftArm.showModel = false;
        this.bipedRightArm.showModel = false;
        this.bipedLeftLeg.showModel = false;
        this.bipedRightLeg.showModel = false;
    }

    private static DebugogglesModel INSTANCE = null;

    public static DebugogglesModel getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new DebugogglesModel(0.0625f, 0, 64, 32);
        }
        return INSTANCE;
    }
}
