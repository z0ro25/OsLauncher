/*
 * Copyright (C) 2018 CyberAgent, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.amz.ios.gpuimage.filter;

/**
 * A more generalized 9x9 Gaussian blur filter
 * blurSize value ranging from 0.0 on up, with a default of 1.0
 */
public class GPUImageGaussianBlurFilter extends GPUImageTwoPassTextureSamplingFilter {
    public static final String VERTEX_SHADER =
            "attribute vec4 position;\n" +
                    "attribute vec4 inputTextureCoordinate;\n" +
                    "\n" +
                    "const int GAUSSIAN_SAMPLES = 9;\n" +
                    "\n" +
                    "uniform float texelWidthOffset;\n" +
                    "uniform float texelHeightOffset;\n" +
                    "\n" +
                    "varying vec2 textureCoordinate;\n" +
                    "varying vec2 blurCoordinates[GAUSSIAN_SAMPLES];\n" +
                    "\n" +
                    "void main()\n" +
                    "{\n" +
                    "	gl_Position = position;\n" +
                    "	textureCoordinate = inputTextureCoordinate.xy;\n" +
                    "	\n" +
                    "	// Calculate the positions for the blur\n" +
                    "	int multiplier = 0;\n" +
                    "	vec2 blurStep;\n" +
                    "   vec2 singleStepOffset = vec2(texelHeightOffset, texelWidthOffset);\n" +
                    "    \n" +
                    "	for (int i = 0; i < GAUSSIAN_SAMPLES; i++)\n" +
                    "   {\n" +
                    "		multiplier = (i - ((GAUSSIAN_SAMPLES - 1) / 2));\n" +
                    "       // Blur in x (horizontal)\n" +
                    "       blurStep = float(multiplier) * singleStepOffset;\n" +
                    "		blurCoordinates[i] = inputTextureCoordinate.xy + blurStep;\n" +
                    "	}\n" +
                    "}\n";

    public static final String FRAGMENT_SHADER =
            "uniform sampler2D inputImageTexture;\n" +
                    "\n" +
                    "const lowp int GAUSSIAN_SAMPLES = 9;\n" +
                    "\n" +
                    "varying highp vec2 textureCoordinate;\n" +
                    "varying highp vec2 blurCoordinates[GAUSSIAN_SAMPLES];\n" +
                    "\n" +
                    "void main()\n" +
                    "{\n" +
                    "	lowp vec3 sum = vec3(0.0);\n" +
                    "   lowp vec4 fragColor=texture2D(inputImageTexture,textureCoordinate);\n" +
                    "	\n" +
                    "    sum += texture2D(inputImageTexture, blurCoordinates[0]).rgb * 0.05;\n" +
                    "    sum += texture2D(inputImageTexture, blurCoordinates[1]).rgb * 0.09;\n" +
                    "    sum += texture2D(inputImageTexture, blurCoordinates[2]).rgb * 0.12;\n" +
                    "    sum += texture2D(inputImageTexture, blurCoordinates[3]).rgb * 0.15;\n" +
                    "    sum += texture2D(inputImageTexture, blurCoordinates[4]).rgb * 0.18;\n" +
                    "    sum += texture2D(inputImageTexture, blurCoordinates[5]).rgb * 0.15;\n" +
                    "    sum += texture2D(inputImageTexture, blurCoordinates[6]).rgb * 0.12;\n" +
                    "    sum += texture2D(inputImageTexture, blurCoordinates[7]).rgb * 0.09;\n" +
                    "    sum += texture2D(inputImageTexture, blurCoordinates[8]).rgb * 0.05;\n" +
                    "\n" +
                    "	gl_FragColor = vec4(sum,fragColor.a);\n" +
                    "}";

    protected float blurSize;

    public GPUImageGaussianBlurFilter() {
        this(1f);
    }

    public GPUImageGaussianBlurFilter(float blurSize) {
        super(VERTEX_SHADER, FRAGMENT_SHADER, VERTEX_SHADER, FRAGMENT_SHADER);
        this.blurSize = blurSize;
    }

    public GPUImageGaussianBlurFilter(String fragmentShaderString, String vertexShaderString, float blurSize) {
        super(vertexShaderString, fragmentShaderString, vertexShaderString, fragmentShaderString);
        this.blurSize = blurSize;
    }


    @Override
    public void onInitialized() {
        super.onInitialized();
        setBlurSize(blurSize);
    }

    @Override
    public float getVerticalTexelOffsetRatio() {
        return blurSize;
    }

    @Override
    public float getHorizontalTexelOffsetRatio() {
        return blurSize;
    }

    /**
     * A multiplier for the blur size, ranging from 0.0 on up, with a default of 1.0
     *
     * @param blurSize from 0.0 on up, default 1.0
     */
    public void setBlurSize(float blurSize) {
        this.blurSize = blurSize;
        runOnDraw(new Runnable() {
            @Override
            public void run() {
                initTexelOffsets();
            }
        });
    }

    public static String vertexShaderForStandardBlurOfRadius(int blurRadius, float sigma)
    {
        if (blurRadius < 1)
        {
            return VERTEX_SHADER;
        }

        String shaderString = "";

        // Header
        shaderString += "attribute vec4 position;\n";
        shaderString += "attribute vec4 inputTextureCoordinate;\n";

        shaderString += "uniform float texelWidthOffset;\n";
        shaderString += "uniform float texelHeightOffset;\n";
        shaderString += "varying vec2 textureCoordinate;\n";
        shaderString += String.format("varying vec2 blurCoordinates[%d];\n", (blurRadius * 2 + 1));

        shaderString += "void main()\n";
        shaderString += "{\n";
        shaderString += "gl_Position = position;\n";
        shaderString += "textureCoordinate = inputTextureCoordinate.xy;\n";
        shaderString += "\n";
        shaderString += "vec2 singleStepOffset = vec2(texelWidthOffset, texelHeightOffset);\n";

        // Inner offset loop
        for (int currentBlurCoordinateIndex = 0; currentBlurCoordinateIndex < (blurRadius * 2 + 1); currentBlurCoordinateIndex++)
        {
            int offsetFromCenter = currentBlurCoordinateIndex - blurRadius;
            if (offsetFromCenter < 0)
            {
                shaderString += String.format("blurCoordinates[%d] = inputTextureCoordinate.xy - singleStepOffset * %f;\n",
                        currentBlurCoordinateIndex,
                        (float)(-offsetFromCenter));
            }
            else if (offsetFromCenter > 0)
            {
                shaderString += String.format("blurCoordinates[%d] = inputTextureCoordinate.xy + singleStepOffset * %f;\n",
                        currentBlurCoordinateIndex, (float)(offsetFromCenter));
            }
            else
            {
                shaderString += String.format("blurCoordinates[%d] = inputTextureCoordinate.xy;\n", currentBlurCoordinateIndex);
            }
        }

            // Footer
        shaderString += "}\n";
        return shaderString;
    }

    public static String fragmentShaderForStandardBlurOfRadius(int blurRadius, float sigma)
    {
        if (blurRadius < 1)
        {
            return FRAGMENT_SHADER;
        }

        // First, generate the normal Gaussian weights for a given sigma
        float standardGaussianWeights[] = new float[blurRadius + 1];
        float sumOfWeights = 0.0f;
        for (int currentGaussianWeightIndex = 0; currentGaussianWeightIndex < blurRadius + 1; currentGaussianWeightIndex++)
        {
            standardGaussianWeights[currentGaussianWeightIndex] = (float)((1.0 / Math.sqrt(2.0 * Math.PI * Math.pow(sigma, 2.0))) * Math.exp(-Math.pow(currentGaussianWeightIndex, 2.0) / (2.0 * Math.pow(sigma, 2.0))));

            if (currentGaussianWeightIndex == 0)
            {
                sumOfWeights += standardGaussianWeights[currentGaussianWeightIndex];
            }
            else
            {
                sumOfWeights += 2.0 * standardGaussianWeights[currentGaussianWeightIndex];
            }
        }

        // Next, normalize these weights to prevent the clipping of the Gaussian curve at the end of the discrete samples from reducing luminance
        for (int currentGaussianWeightIndex = 0; currentGaussianWeightIndex < blurRadius + 1; currentGaussianWeightIndex++)
        {
            standardGaussianWeights[currentGaussianWeightIndex] = standardGaussianWeights[currentGaussianWeightIndex] / sumOfWeights;
        }

        // Finally, generate the shader from these weights
        String shaderString = "";
        // Header
        shaderString += "uniform highp sampler2D inputImageTexture;\n";
        shaderString += "varying highp vec2 textureCoordinate;\n";
        shaderString += String.format("varying highp vec2 blurCoordinates[%d];\n", (blurRadius * 2 + 1));
        shaderString += "void main()\n";
        shaderString += "{\n";
        shaderString += "highp vec3 sum = vec3(0.0);\n";
        shaderString += "highp vec4 fragColor=texture2D(inputImageTexture,textureCoordinate);\n";


        // Inner texture loop
        for (int currentBlurCoordinateIndex = 0; currentBlurCoordinateIndex < (blurRadius * 2 + 1); currentBlurCoordinateIndex++)
        {
            int offsetFromCenter = currentBlurCoordinateIndex - blurRadius;
            if (offsetFromCenter < 0)
            {
                shaderString += String.format("sum += texture2D(inputImageTexture, blurCoordinates[%d]).rgb * %f;\n", currentBlurCoordinateIndex, standardGaussianWeights[-offsetFromCenter]);
            }
            else
            {
                shaderString += String.format("sum += texture2D(inputImageTexture, blurCoordinates[%d]).rgb * %f;\n", currentBlurCoordinateIndex, standardGaussianWeights[offsetFromCenter]);
            }
        }

        // Footer
        shaderString += "gl_FragColor = vec4(sum, fragColor.a);\n";
        shaderString += "}";
        return shaderString;
    }

    public static String vertexShaderForOptimizedBlurOfRadius(int blurRadius, float sigma) {
        if (blurRadius < 1) {
            return VERTEX_SHADER;
        }

        // First, generate the normal Gaussian weights for a given sigma
        float standardGaussianWeights[] = new float[blurRadius + 1];
        float sumOfWeights = 0.0f;
        for (int currentGaussianWeightIndex = 0; currentGaussianWeightIndex < blurRadius + 1; currentGaussianWeightIndex++) {
            standardGaussianWeights[currentGaussianWeightIndex] = (float) ((1.0 / Math.sqrt(2.0 * Math.PI * Math.pow(sigma, 2.0))) * Math.exp(-Math.pow(currentGaussianWeightIndex, 2.0) / (2.0 * Math.pow(sigma, 2.0))));

            if (currentGaussianWeightIndex == 0) {
                sumOfWeights += standardGaussianWeights[currentGaussianWeightIndex];
            } else {
                sumOfWeights += 2.0 * standardGaussianWeights[currentGaussianWeightIndex];
            }
        }

        // Next, normalize these weights to prevent the clipping of the Gaussian curve at the end of the discrete samples from reducing luminance
        for (int currentGaussianWeightIndex = 0; currentGaussianWeightIndex < blurRadius + 1; currentGaussianWeightIndex++) {
            standardGaussianWeights[currentGaussianWeightIndex] = standardGaussianWeights[currentGaussianWeightIndex] / sumOfWeights;
        }

        // From these weights we calculate the offsets to read interpolated values from
        int numberOfOptimizedOffsets = Math.min(blurRadius / 2 + (blurRadius % 2), 7);
        float optimizedGaussianOffsets[] = new float[numberOfOptimizedOffsets];

        for (int currentOptimizedOffset = 0; currentOptimizedOffset < numberOfOptimizedOffsets; currentOptimizedOffset++) {
            float firstWeight = standardGaussianWeights[currentOptimizedOffset * 2 + 1];
            float secondWeight = standardGaussianWeights[currentOptimizedOffset * 2 + 2];

            float optimizedWeight = firstWeight + secondWeight;

            optimizedGaussianOffsets[currentOptimizedOffset] = (firstWeight * (currentOptimizedOffset * 2 + 1) + secondWeight * (currentOptimizedOffset * 2 + 2)) / optimizedWeight;
        }

        String shaderString = "";
        // Header
        shaderString += "attribute vec4 position;\n";
        shaderString += "attribute vec4 inputTextureCoordinate;\n";
        shaderString += "\n";
        shaderString += "uniform float texelWidthOffset;\n";
        shaderString += "uniform float texelHeightOffset;\n";
        shaderString += "\n";
        shaderString += String.format("varying vec2 blurCoordinates[%d];\n", (1 + (numberOfOptimizedOffsets * 2)));
        shaderString += "\n";
        shaderString += "void main()\n";
        shaderString += "{\n";
        shaderString += "   gl_Position = position;\n";
        shaderString += "   \n";
        shaderString += "   vec2 singleStepOffset = vec2(texelWidthOffset, texelHeightOffset);\n";
        // Inner offset loop
        shaderString += "blurCoordinates[0] = inputTextureCoordinate.xy;\n";
        for (int currentOptimizedOffset = 0; currentOptimizedOffset < numberOfOptimizedOffsets; currentOptimizedOffset++) {
            shaderString += String.format("blurCoordinates[%d] = inputTextureCoordinate.xy + singleStepOffset * %f;\nblurCoordinates[%d] = inputTextureCoordinate.xy - singleStepOffset * %f;\n",
                    ((currentOptimizedOffset * 2) + 1),
                    optimizedGaussianOffsets[currentOptimizedOffset],
                    ((currentOptimizedOffset * 2) + 2),
                    optimizedGaussianOffsets[currentOptimizedOffset]);
        }

        // Footer
        shaderString += "}\n";
        return shaderString;
    }

    public static String fragmentShaderForOptimizedBlurOfRadius(int blurRadius, float sigma)
    {
        if (blurRadius < 1)
        {
            return FRAGMENT_SHADER;
        }

        // First, generate the normal Gaussian weights for a given sigma
        float standardGaussianWeights[] = new float[blurRadius + 1];
        float sumOfWeights = 0.0f;
        for (int currentGaussianWeightIndex = 0; currentGaussianWeightIndex < blurRadius + 1; currentGaussianWeightIndex++)
        {
            standardGaussianWeights[currentGaussianWeightIndex] = (float)((1.0 / Math.sqrt(2.0 * Math.PI * Math.pow(sigma, 2.0))) * Math.exp(-Math.pow(currentGaussianWeightIndex, 2.0) / (2.0 * Math.pow(sigma, 2.0))));

            if (currentGaussianWeightIndex == 0)
            {
                sumOfWeights += standardGaussianWeights[currentGaussianWeightIndex];
            }
            else
            {
                sumOfWeights += 2.0 * standardGaussianWeights[currentGaussianWeightIndex];
            }
        }

        // Next, normalize these weights to prevent the clipping of the Gaussian curve at the end of the discrete samples from reducing luminance
        for (int currentGaussianWeightIndex = 0; currentGaussianWeightIndex < blurRadius + 1; currentGaussianWeightIndex++)
        {
            standardGaussianWeights[currentGaussianWeightIndex] = standardGaussianWeights[currentGaussianWeightIndex] / sumOfWeights;
        }

        // From these weights we calculate the offsets to read interpolated values from
        int numberOfOptimizedOffsets = Math.min(blurRadius / 2 + (blurRadius % 2), 7);
        int trueNumberOfOptimizedOffsets = blurRadius / 2 + (blurRadius % 2);

        String shaderString = "";

        // Header
        shaderString += "uniform highp sampler2D inputImageTexture;\n";
        shaderString += "uniform float texelWidthOffset;\n";
        shaderString += "uniform float texelHeightOffset;\n";
        shaderString += "\n";
        shaderString += String.format("varying vec2 blurCoordinates[%d];\n", 1 + (numberOfOptimizedOffsets * 2));
        shaderString += "\n";
        shaderString += "void main()\n";
        shaderString += "{\n";
        shaderString += "highp vec4 sum = vec4(0.0);\n";

        // Inner texture loop
        shaderString += String.format("sum += texture2D(inputImageTexture, blurCoordinates[0]) * %f;\n", standardGaussianWeights[0]);

        for (int currentBlurCoordinateIndex = 0; currentBlurCoordinateIndex < numberOfOptimizedOffsets; currentBlurCoordinateIndex++)
        {
            float firstWeight = standardGaussianWeights[currentBlurCoordinateIndex * 2 + 1];
            float secondWeight = standardGaussianWeights[currentBlurCoordinateIndex * 2 + 2];
            float optimizedWeight = firstWeight + secondWeight;

            shaderString += String.format("sum += texture2D(inputImageTexture, blurCoordinates[%d]) * %f;\n",
                    ((currentBlurCoordinateIndex * 2) + 1), optimizedWeight);
            shaderString += String.format("sum += texture2D(inputImageTexture, blurCoordinates[%d]) * %f;\n",
                    ((currentBlurCoordinateIndex * 2) + 2), optimizedWeight);
        }

        // If the number of required samples exceeds the amount we can pass in via varyings, we have to do dependent texture reads in the fragment shader
        if (trueNumberOfOptimizedOffsets > numberOfOptimizedOffsets)
        {
            shaderString += "highp vec2 singleStepOffset = vec2(texelWidthOffset, texelHeightOffset);\n";

            for (int currentOverlowTextureRead = numberOfOptimizedOffsets; currentOverlowTextureRead < trueNumberOfOptimizedOffsets; currentOverlowTextureRead++)
            {
                float firstWeight = standardGaussianWeights[currentOverlowTextureRead * 2 + 1];
                float secondWeight = standardGaussianWeights[currentOverlowTextureRead * 2 + 2];

                float optimizedWeight = firstWeight + secondWeight;
                float optimizedOffset = (firstWeight * (currentOverlowTextureRead * 2 + 1) + secondWeight * (currentOverlowTextureRead * 2 + 2)) / optimizedWeight;

                shaderString += String.format("sum += texture2D(inputImageTexture, blurCoordinates[0] + singleStepOffset * %f) * %f;\n", optimizedOffset, optimizedWeight);
                shaderString += String.format("sum += texture2D(inputImageTexture, blurCoordinates[0] - singleStepOffset * %f) * %f;\n", optimizedOffset, optimizedWeight);
            }
        }

                // Footer
        shaderString += "gl_FragColor = sum;\n";
        shaderString += "}\n";
        return shaderString;
    }
}
