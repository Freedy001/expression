import javax.swing.*;
import java.awt.*;
import java.io.File;

/**
 * @author Freedy
 * @date 2022/12/25 21:30
 */
public class Zoom extends JFrame {
    JPanel p;
    JLabel jl;
    File[] files;
    ImageIcon icon = null;
    double x3, y3, oldImgWidth, oldImgHeight, zoomRatioWidth, zoomRatioHeight;
    int imgWidth, imgHeight;
    private static int c = 0;

    public Zoom(File pictureFolder) {
        files = pictureFolder.listFiles(f -> f.isFile() && f.getName().contains(".png"));// files 文件数组
        setSize(500, 500); // 设置窗体大小
        init(); // 创建组件
        setLayout(); // 调用布局
        setAttribute(); // 调用属性
        setListener(); // 调用监听
        setLocationRelativeTo(null); // 窗体居中
        setVisible(true); // 窗体显示
    }

    private void init() { // 初始化创建组件
        p = new JPanel(); // 创建组件
        jl = new JLabel(); // 创建组件
    }

    private void setLayout() { // 窗体组件布局
        p.removeAll(); // 布局前清空 p
        p.setLayout(null);// 无布局样式
        if (icon == null)
            icon = new ImageIcon(files[c].getAbsolutePath()); // 图片容器
        jl.setIcon(icon); // jl 设置图片
        jl.setHorizontalAlignment(SwingConstants.CENTER); // jl 设置图片水平居中
        jl.setSize(icon.getIconWidth(), icon.getIconHeight()); // 以图片的宽高设置 jl 的宽高
        p.add(jl); // 添加
        add(p); // 添加
        repaint(); // 刷新
        validate(); // 刷新
    }

    private void setAttribute() {// 设置窗体组件属性
    }

    private void setListener() {
        jl.addMouseWheelListener(e -> { // 鼠标滚轮监听
            if (e.getWheelRotation() == -1) { // 滚轮向外滚
                oldImgWidth = icon.getIconWidth(); // 放大前的 icon 图片，备用
                oldImgHeight = icon.getIconHeight(); // 放大前的 icon 图片，备用
                imgWidth = icon.getIconWidth(); // 放大前的 icon 图片，备用
                imgHeight = icon.getIconHeight(); // 放大前的 icon 图片，备用
                double num = imgWidth / imgHeight; // num 宽高比
                imgWidth += 100; // 设置放大的宽
                imgHeight += (int) (100 / num);// 设置放大的高
                if (imgWidth < getWidth() * 10) { // 宽度小于窗体10倍执行
                    icon = new ImageIcon(files[c].getAbsolutePath()); // new icon，避免丢失像素
                    icon = new ImageIcon(icon.getImage().getScaledInstance(imgWidth, imgHeight, Image.SCALE_DEFAULT)); // 放大
                    setLayout(); // 重新布局

                    /*
                     * douzoomRatio 缩放比 （ new icon ）：（ old icon） 这样可以用，反过来就有问题了。 x3,y3
                     * 扒了（CSDN）大佬（VIctor_Ye）的算法用的,非常感谢！！！
                     */ double douzoomRatio = Math.sqrt(imgWidth * imgWidth + imgHeight * imgHeight)
                            / Math.sqrt(oldImgWidth * oldImgWidth + oldImgHeight * oldImgHeight);
                    x3 = (1 - douzoomRatio) * (getMousePosition().x - jl.getBounds().x) + jl.getBounds().x;
                    y3 = (1 - douzoomRatio) * (getMousePosition().y - jl.getBounds().y) + jl.getBounds().y;
                    jl.setLocation((int) x3, (int) (y3));// 设置 jl 的位置
                    repaint(); // 刷新
                    validate(); // 刷新
                }
            }
            if (e.getWheelRotation() == 1) { // 滚轮向内滚
                oldImgWidth = icon.getIconWidth();
                oldImgHeight = icon.getIconHeight();
                imgWidth = icon.getIconWidth();
                imgHeight = icon.getIconHeight();
                double num = imgWidth / imgHeight;
                imgWidth -= 100; // 缩小
                imgHeight -= (int) (100 / num);// 缩小
                if (imgWidth > getWidth() / 5) { // 宽度大于窗体 1/5 执行
                    icon = new ImageIcon(files[c].getAbsolutePath());
                    icon = new ImageIcon(icon.getImage().getScaledInstance(imgWidth, imgHeight, Image.SCALE_DEFAULT));
                    setLayout();
                    double douzoomRatio = Math.sqrt(imgWidth * imgWidth + imgHeight * imgHeight)
                            / Math.sqrt(oldImgWidth * oldImgWidth + oldImgHeight * oldImgHeight);
                    x3 = (1 - douzoomRatio) * (getMousePosition().x - jl.getBounds().x) + jl.getBounds().x;
                    y3 = (1 - douzoomRatio) * (getMousePosition().y - jl.getBounds().y) + jl.getBounds().y;
                    jl.setLocation((int) x3, (int) (y3));
                    repaint();
                    validate();
                }
            }
        });

    }

    public static void main(String[] args) { // 主方法
        File pictureFolder = new File("C:\\Users\\Freedy\\Desktop\\code\\expression\\src\\test\\java\\"); // E 盘的图片文件夹的路径，注意“\\”或“/”
        new Zoom(pictureFolder); // new 构造方法
    }
}
