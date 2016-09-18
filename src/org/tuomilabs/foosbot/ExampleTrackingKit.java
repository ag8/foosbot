package org.tuomilabs.foosbot;

import boofcv.alg.tracker.meanshift.PixelLikelihood;
import boofcv.alg.tracker.meanshift.TrackerMeanShiftLikelihood;
import boofcv.gui.image.ShowImages;
import boofcv.gui.tracker.TrackerObjectQuadPanel;
import boofcv.io.MediaManager;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.wrapper.DefaultMediaManager;
import boofcv.misc.BoofMiscOps;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageType;
import boofcv.struct.image.Planar;
import georegression.struct.shapes.RectangleLength2D_I32;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.Date;

public class ExampleTrackingKit {
    static int fuck = 1920;
    static int shit = 1080;

    public static int[] position = new int[2];

    public static void main(String[] args) throws IOException {
        MediaManager media = DefaultMediaManager.INSTANCE;
//        String fileName = UtilIO.pathExample("tracking/balls_blue_red.mjpeg");
        RectangleLength2D_I32 location = new RectangleLength2D_I32(320, 240, 50, 50);

        ImageType<Planar<GrayU8>> imageType = ImageType.pl(3, GrayU8.class);

//        SimpleImageSequence<Planar<GrayU8>> video = media.openVideo(fileName, imageType);

        // Return a higher likelihood for pixels close to this RGB color
        RgbLikelihood likelihood = new RgbLikelihood(107, 118, 238);

        TrackerMeanShiftLikelihood<Planar<GrayU8>> tracker =
                new TrackerMeanShiftLikelihood<>(likelihood, 50, 0.1f);

        // specify the target's initial location and initialize with the first frame
        Planar<GrayU8> frame = ConvertBufferedImage.convertFromMulti(getImageBg(), null, true, GrayU8.class);

        // Note that the tracker will not automatically invoke RgbLikelihood.createModel() in its initialize function
        tracker.initialize(frame, location);

        // For displaying the results
        TrackerObjectQuadPanel gui = new TrackerObjectQuadPanel(null);
        gui.setPreferredSize(new Dimension(frame.getWidth(), frame.getHeight()));
        gui.setBackGround(getImageBg());
        gui.setTarget(location, true);
        ShowImages.showWindow(gui, "Tracking Results", true);

        // Track the object across each video frame and display the results
        while (true) {
            frame = ConvertBufferedImage.convertFromMulti(getImageBg(), null, true, GrayU8.class);

            boolean visible = tracker.process(frame);

            if (!visible) {
                System.out.println("So, the visibility is false.");
                for (int i = 0; i < fuck - 10; i++) {
                    for (int j = 0; j < shit - 10; j++) {
                        tracker.getLocation().setX(990);
                        tracker.getLocation().setY(520);
                        System.out.println(tracker.getLocation());
                        visible = tracker.process(frame);

                        if (visible) {
                            System.out.println("FOUND IT!!!! AT " + i + ", " + j + "!!!!!!!!!");
                            break;
//                        } else {
//                            System.out.println("Yeah... nothing at " + i + ", " + j + ".");
                        }
                    }
                }
            }



            gui.setBackGround(getImageBg());
            System.out.println(tracker.getLocation().toString() + ". (Visibility: " + visible + ")");
            gui.setTarget(tracker.getLocation(), visible);
            gui.repaint();


            position[0] = tracker.getLocation().getX();
            position[1] = tracker.getLocation().getY();
            tracker.getLocation().setX(position[0]);
            tracker.getLocation().setY(position[1]);

//            tracker.getLocation().setX(3);
//            System.out.println(likelihood.compute(1, 2));


//            BoofMiscOps.pause(20);
        }
    }

    public static BufferedImage getImageBg() throws IOException {
        return ImageIO.read(new URL("http://10.0.0.70:25522/shot.jpg?"/* + new Date().getTime()*/));
    }

    /**
     * Very simple implementation of PixelLikelihood.  Uses linear distance to compute how close
     * a color is to the target color.
     */
    public static class RgbLikelihood implements PixelLikelihood<Planar<GrayU8>> {

        int targetRed, targetGreen, targetBlue;
        float radius = 50;
        Planar<GrayU8> image;

        public RgbLikelihood(int targetRed, int targetGreen, int targetBlue) {
            this.targetRed = targetRed;
            this.targetGreen = targetGreen;
            this.targetBlue = targetBlue;
        }

        @Override
        public void setImage(Planar<GrayU8> image) {
            this.image = image;
        }

        @Override
        public boolean isInBounds(int x, int y) {
            return image.isInBounds(x, y);
        }

        /**
         * This function is used to learn the target's model from the select image region.  Since the
         * model is provided in the constructor it isn't needed or used.
         */
        @Override
        public void createModel(RectangleLength2D_I32 target) {
            throw new RuntimeException("Not supported");
        }

        @Override
        public float compute(int x, int y) {
            int pixelR = image.getBand(0).get(x, y);
            int pixelG = image.getBand(1).get(x, y);
            int pixelB = image.getBand(2).get(x, y);

            // distance along each color band
            float red = Math.max(0, 1.0f - Math.abs(targetRed - pixelR) / radius);
            float green = Math.max(0, 1.0f - Math.abs(targetGreen - pixelG) / radius);
            float blue = Math.max(0, 1.0f - Math.abs(targetBlue - pixelB) / radius);

            // multiply them all together
            return red * green * blue;
        }
    }
}
