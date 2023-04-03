import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdfviewer.PageDrawer;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.PDGraphicsState;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAppearanceDictionary;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAppearanceStream;

public class AnnotationDrawer extends PageDrawer
{
    private static final int DEFAULT_USER_SPACE_UNIT_DPI = 72;
    private static final Color TRANSPARENT_WHITE = new Color( 255, 255, 255, 0 );

    private int imageType;
    private int resolution;

    public AnnotationDrawer(int imageType, int resolution) throws IOException
    {
        super();
        this.imageType = imageType;
        this.resolution = resolution;
    }

    public Map<String, BufferedImage> convertToImages(PDPage p) throws IOException
    {
        page = p;
        final Map<String, BufferedImage> result = new HashMap<String, BufferedImage>();

        List<PDAnnotation> annotations = page.getAnnotations();
        for (PDAnnotation annotation: annotations)
        {
            String appearanceName = annotation.getAppearanceStream();
            PDAppearanceDictionary appearDictionary = annotation.getAppearance();
            if( appearDictionary != null )
            {
                if( appearanceName == null )
                {
                    appearanceName = "default";
                }
                Map<String, PDAppearanceStream> appearanceMap = appearDictionary.getNormalAppearance();
                if (appearanceMap != null) 
                {
                    PDAppearanceStream appearance = 
                        (PDAppearanceStream)appearanceMap.get( appearanceName ); 
                    if( appearance != null ) 
                    {
                        BufferedImage image = initializeGraphics(annotation);
                        setTextMatrix(null);
                        setTextLineMatrix(null);
                        getGraphicsStack().clear();
                        processSubStream( page, appearance.getResources(), appearance.getStream() );

                        String name = annotation.getAnnotationName();
                        if (name == null || name.length() == 0)
                        {
                            name = annotation.getDictionary().getString(COSName.T);
                            if (name == null || name.length() == 0)
                            {
                                name = Long.toHexString(annotation.hashCode());
                            }
                        }

                        result.put(name, image);
                    }
                }
            }
        }

        return result;
    }

    BufferedImage initializeGraphics(PDAnnotation annotation)
    {
        PDRectangle rect = annotation.getRectangle();
        float widthPt = rect.getWidth();
        float heightPt = rect.getHeight();
        float scaling = resolution / (float)DEFAULT_USER_SPACE_UNIT_DPI;
        int widthPx = Math.round(widthPt * scaling);
        int heightPx = Math.round(heightPt * scaling);
        //TODO The following reduces accuracy. It should really be a Dimension2D.Float.
        Dimension pageDimension = new Dimension( (int)widthPt, (int)heightPt );
        BufferedImage retval = new BufferedImage( widthPx, heightPx, imageType );
        Graphics2D graphics = (Graphics2D)retval.getGraphics();
        graphics.setBackground( TRANSPARENT_WHITE );
        graphics.clearRect( 0, 0, retval.getWidth(), retval.getHeight() );
        graphics.scale( scaling, scaling );
        setGraphics(graphics);
        pageSize = pageDimension;
        graphics.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON );
        graphics.setRenderingHint( RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON );
        setGraphicsState(new PDGraphicsState(new PDRectangle(widthPt, heightPt)));

        return retval;
    }

    void setGraphics(Graphics2D graphics)
    {
        try {
            Field field = PageDrawer.class.getDeclaredField("graphics");
            field.setAccessible(true);
            field.set(this, graphics);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}