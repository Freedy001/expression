import lombok.SneakyThrows;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.*;

/**
 * @author Freedy
 * @date 2022/9/28 23:14
 */
public class Jdk19Test {

    public static final int scaled = 3;

    private final static String p1="C:\\Users\\Freedy\\Desktop\\code\\expression\\src\\test\\java\\uper1.png";
    private final static String p2="C:\\Users\\Freedy\\Desktop\\code\\expression\\src\\test\\java\\down2.png";

    @SneakyThrows
    public static void main(String[] args) {
        BufferedImage image1 = ImageIO.read(new File(p1));
        BufferedImage image2 = ImageIO.read(new File(p2));

        int width = image1.getWidth();
        if (image2.getWidth() != width) throw new IllegalArgumentException();
        int height1 = image1.getHeight();
        int height2 = image2.getHeight();

        LinkedHashMap<ArrayList<Integer>, Integer> set = new LinkedHashMap<>();


        for (int line = 0; line < height1; line++) {
            ArrayList<Integer> lineRgb = new ArrayList<>();
            for (int col = 0; col < width; col++) {
                lineRgb.add(image1.getRGB(col, line));
            }
            set.put(lineRgb, line);
        }

        ArrayList<Integer> sameRgbLine1 = new ArrayList<>();
        ArrayList<Integer> sameRgbLine2 = new ArrayList<>();

        for (int line = 0; line < height2; line++) {
            ArrayList<Integer> lineRgb = new ArrayList<>();
            for (int col = 0; col < width; col++) {
                lineRgb.add(image2.getRGB(col, line));
            }
            Integer rgbLine1 = set.get(lineRgb);
            if (rgbLine1 != null) {
                sameRgbLine1.add(rgbLine1);
                sameRgbLine2.add(line);
            }
        }

        TreeMap<Integer, PriorityQueue<Integer>> gapLine1 = new TreeMap<>();
        TreeMap<Integer, PriorityQueue<Integer>> gapLine2 = new TreeMap<>();

        for (int i = 0; i < sameRgbLine1.size() - 1; i++) {
            int finalI = i;
            gapLine1.compute(sameRgbLine1.get(i + 1) - sameRgbLine1.get(i), (k, v) -> {
                if (v == null) return new PriorityQueue<>(List.of(sameRgbLine1.get(finalI)));
                v.add(sameRgbLine1.get(finalI));
                return v;
            });
            gapLine2.compute(sameRgbLine2.get(i + 1) - sameRgbLine2.get(i), (k, v) -> {
                if (v == null) return new PriorityQueue<>(List.of(sameRgbLine2.get(finalI)));
                v.add(sameRgbLine2.get(finalI));
                return v;
            });
        }

        PriorityQueue<Integer> value1 = gapLine1.firstEntry().getValue();
        PriorityQueue<Integer> value2 = gapLine2.firstEntry().getValue();

        Integer first1 = value1.poll();
        Integer first2 = value2.poll();

        assert first1 != null;
        assert first2 != null;

        BufferedImage image = new BufferedImage(width, first1 + (height2 - first2), BufferedImage.TYPE_INT_ARGB);
        int imageLine = 0;
        for (; imageLine < first1; imageLine++) {
            for (int col = 0; col < width; col++) {
                image.setRGB(col, imageLine, image1.getRGB(col, imageLine));
            }
        }

        for (int line = first2; line < height2; line++, imageLine++) {
            for (int col = 0; col < width; col++) {
                image.setRGB(col, imageLine, image2.getRGB(col, line));
            }
        }


//        int l = 0;
//        for (ArrayList<Integer> line : set) {
//            for (int col = 1; col < line.size(); col++) {
//                image.setRGB(col-1, l, line.get(col));
//            }
//            l++;
//        }

        out(image);
        outF(image);
    }


//    public static int pixel(int[] p){
//
//    }

    public static void out(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        JFrame jFrame = new JFrame();
        jFrame.add(new JLabel(new ImageIcon(image.getScaledInstance(width / scaled, height / scaled, Image.SCALE_DEFAULT))));
        jFrame.setSize(width / scaled, height / scaled);
        jFrame.setVisible(true);

    }


    public static void outF(BufferedImage image) {

        // write image
        try {
            File f = new File("./aaa.png");
            ImageIO.write(image, "png", f);
            Runtime.getRuntime().exec("cmd /k start aaa.png");
        } catch (IOException e) {
            System.out.println("Error: " + e);
        }
    }

}
