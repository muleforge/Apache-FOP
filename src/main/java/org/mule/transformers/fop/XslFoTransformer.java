package org.mule.transformers.fop;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.fop.apps.FOPException;
import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.apps.Fop;
import org.apache.fop.apps.FopFactory;
import org.apache.fop.apps.MimeConstants;
import org.mule.RequestContext;
import org.mule.api.transformer.DataType;
import org.mule.api.transport.PropertyScope;
import org.mule.transformer.AbstractTransformer;

public class XslFoTransformer extends AbstractTransformer {

  private FopFactory fopFactory = FopFactory.newInstance();
  
  private String foFile = null;
  private String mimeType = MimeConstants.MIME_PDF;
  private String targetResolution = "72.0";
  
  public XslFoTransformer() {
    registerSourceType(DataType.BYTE_ARRAY_DATA_TYPE);
    registerSourceType(DataType.STRING_DATA_TYPE);
    setReturnDataType(DataType.BYTE_ARRAY_DATA_TYPE);
  }

  public Object doTransform(Object source, String encoding) {
    InputStream is = null;
    //source = "<test></test>";
    if (source instanceof String)
      is = new ByteArrayInputStream(((String)source).getBytes());
    else
      is = new ByteArrayInputStream((byte [])source);
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    fopFactory.setTargetResolution(Float.parseFloat(getTargetResolution()));
    FOUserAgent foUserAgent = fopFactory.newFOUserAgent();
    byte[] result = null;
    try {
      Fop fop = fopFactory.newFop(getMimeType(), foUserAgent, out);
      Source src = new StreamSource(is);
      TransformerFactory factory = TransformerFactory.newInstance();
      Transformer transformer = null;
      if (getFoFile() == null || getFoFile().length() == 0)
        transformer = factory.newTransformer();
      else
        transformer = factory.newTransformer(new StreamSource(foFile));
      Result res = new SAXResult(fop.getDefaultHandler());
      transformer.transform(src, res);
      result = out.toByteArray();
      out.close();
    } catch (IOException ioe) {
      logger.error("An exception occurred while closing the output stream");
    } catch (FOPException fope) {
      logger.error("An exception occurred while creating the FOP object");
    } catch (TransformerConfigurationException tce) {
      logger.error("An exception occurred while configuring the transformer");
    } catch (TransformerException te) {
      logger.error("An exception occurred while transforming the object");
      te.printStackTrace();
    }
    if (logger.isDebugEnabled()) {
      logger.debug("CONTENT TYPE: " + RequestContext.getEvent().getMessage().getProperty("Content-Type", PropertyScope.INBOUND));
      logger.debug("MIME TYPE: " + mimeType);
    }
    RequestContext.getEvent().getMessage().setProperty("Content-Type", mimeType, PropertyScope.OUTBOUND);
    return result;      
  }

  public String getFoFile() {
    return foFile;
  }

  public void setFoFile(String foFile) {
    this.foFile = foFile;
  }

  public String getMimeType() {
    return mimeType;
  }

  public void setMimeType(String mimeType) {
    this.mimeType = mimeType;
  }
  
  public String getTargetResolution() {
    return targetResolution;
  }
  
  public void setTargetResolution(String targetResolution) {
    // This might need changed. I don't necessarily like checking a numeric
    // by catching an exception, but here it goes.
    try {
      float temp = Float.parseFloat(targetResolution);
      logger.debug("Target Resolution: " + temp);
      this.targetResolution = targetResolution;
    } catch (NumberFormatException e) {
      logger.error("The value for target resolution is invaid. Value "
                     .concat(targetResolution));      
    }
  }
}
