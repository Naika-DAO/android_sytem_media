/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package android.filterpacks.imageproc;

import android.filterfw.core.Filter;
import android.filterfw.core.FilterContext;
import android.filterfw.core.Frame;
import android.filterfw.core.FrameFormat;
import android.filterfw.core.GenerateFieldPort;
import android.filterfw.core.KeyValueMap;
import android.filterfw.core.MutableFrameFormat;
import android.filterfw.core.NativeProgram;
import android.filterfw.core.NativeFrame;
import android.filterfw.core.Program;
import android.filterfw.core.ShaderProgram;
import android.filterfw.geometry.Point;
import android.filterfw.geometry.Quad;
import android.filterfw.format.ImageFormat;
import android.filterfw.format.ObjectFormat;

import android.util.Log;

public class CropFilter extends Filter {

    private Program mProgram;

    @GenerateFieldPort(name = "owidth")
    private int mOutputWidth = -1;

    @GenerateFieldPort(name = "oheight")
    private int mOutputHeight = -1;

    public CropFilter(String name) {
        super(name);
    }

    @Override
    public void setupPorts() {
        addMaskedInputPort("image", ImageFormat.create(ImageFormat.COLORSPACE_RGBA));
        addMaskedInputPort("box", ObjectFormat.fromClass(Quad.class, FrameFormat.TARGET_JAVA));
        addOutputBasedOnInput("image", "image");
    }

    @Override
    public FrameFormat getOutputFormat(String portName, FrameFormat inputFormat) {
        // Make sure output size is set to unspecified, as we do not know what we will be resizing
        // to.
        MutableFrameFormat outputFormat = inputFormat.mutableCopy();
        outputFormat.setDimensions(FrameFormat.SIZE_UNSPECIFIED, FrameFormat.SIZE_UNSPECIFIED);
        return outputFormat;
    }

    @Override
    public void prepare(FilterContext env) {
        // TODO: Add CPU version
        switch (getInputFormat("image").getTarget()) {
            case FrameFormat.TARGET_GPU:
                mProgram = ShaderProgram.createIdentity();
                break;
        }
        if (mProgram == null) {
            throw new RuntimeException("Could not create a program for crop filter " + this + "!");
        }
    }

    @Override
    public void process(FilterContext env) {
        // Get input frame
        Frame imageFrame = pullInput("image");
        Frame boxFrame = pullInput("box");

        // Get the box
        Quad box = (Quad)boxFrame.getObjectValue();

        // Create output format
        MutableFrameFormat outputFormat = imageFrame.getFormat().mutableCopy();
        outputFormat.setDimensions(mOutputWidth == -1 ? outputFormat.getWidth() : mOutputWidth,
                                   mOutputHeight == -1 ? outputFormat.getHeight() : mOutputHeight);

        // Create output frame
        Frame output = env.getFrameManager().newFrame(outputFormat);

        // Set the program parameters
        if (mProgram instanceof ShaderProgram) {
            ShaderProgram shaderProgram = (ShaderProgram)mProgram;
            shaderProgram.setSourceRegion(box);
        }

        mProgram.process(imageFrame, output);

        // Push output
        pushOutput("image", output);

        // Release pushed frame
        output.release();
    }


}