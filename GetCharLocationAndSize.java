import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
  
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.List;
  
/**
* This is an example on how to get the x/y coordinates and size of each character in PDF
*/
public class GetCharLocationAndSize extends PDFTextStripper {
  
    public GetCharLocationAndSize() throws IOException {
    }
  
    /**
     * @throws IOException If there is an error parsing the document.
     */
    public static void main( String[] args ) throws IOException {
        PDDocument document = null;
        String fileName = "apache.pdf";
        try {
            document = PDDocument.load( new File(fileName) );
            PDFTextStripper stripper = new GetCharLocationAndSize();
            stripper.setSortByPosition( true );
            stripper.setStartPage( 0 );
            stripper.setEndPage( document.getNumberOfPages() );
  
            Writer dummy = new OutputStreamWriter(new ByteArrayOutputStream());
            stripper.writeText(document, dummy);
        }
        finally {
            if( document != null ) {
                document.close();
            }
        }
    }
  
    /**
     * Override the default functionality of PDFTextStripper.writeString()
     */
    @Override
    protected void writeString(String string, List<TextPosition> textPositions) throws IOException {
        for (TextPosition text : textPositions) {
            System.out.println(text.getUnicode()+ " [(X=" + text.getXDirAdj() + ",Y=" +
                    text.getYDirAdj() + ") height=" + text.getHeightDir() + " width=" +
                    text.getWidthDirAdj() + "]");
        }
    }

    List<TextPositionSequence> findSubwords(PDDocument document, int page, String searchTerm) throws IOException
    {
        final List<TextPosition> allTextPositions = new ArrayList<>();
        PDFTextStripper stripper = new PDFTextStripper()
        {
            @Override
            protected void writeString(String text, List<TextPosition> textPositions) throws IOException
            {
                allTextPositions.addAll(textPositions);
                super.writeString(text, textPositions);
            }
    
            @Override
            protected void writeLineSeparator() throws IOException {
                if (!allTextPositions.isEmpty()) {
                    TextPosition last = allTextPositions.get(allTextPositions.size() - 1);
                    if (!" ".equals(last.getUnicode())) {
                        Matrix textMatrix = last.getTextMatrix().clone();
                        textMatrix.setValue(2, 0, last.getEndX());
                        textMatrix.setValue(2, 1, last.getEndY());
                        TextPosition separatorSpace = new TextPosition(last.getRotation(), last.getPageWidth(), last.getPageHeight(),
                                textMatrix, last.getEndX(), last.getEndY(), last.getHeight(), 0, last.getWidthOfSpace(), " ",
                                new int[] {' '}, last.getFont(), last.getFontSize(), (int) last.getFontSizeInPt());
                        allTextPositions.add(separatorSpace);
                    }
                }
                super.writeLineSeparator();
            }
        };
        
        stripper.setSortByPosition(true);
        stripper.setStartPage(page);
        stripper.setEndPage(page);
        stripper.getText(document);
    
        final List<TextPositionSequence> hits = new ArrayList<TextPositionSequence>();
        TextPositionSequence word = new TextPositionSequence(allTextPositions);
        String string = word.toString();
    
        int fromIndex = 0;
        int index;
        while ((index = string.indexOf(searchTerm, fromIndex)) > -1)
        {
            hits.add(word.subSequence(index, index + searchTerm.length()));
            fromIndex = index + 1;
        }
    
        return hits;
    }
}