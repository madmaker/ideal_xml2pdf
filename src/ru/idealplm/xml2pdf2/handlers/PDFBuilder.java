package ru.idealplm.xml2pdf2.handlers;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.configuration.DefaultConfigurationBuilder;
import org.apache.fop.apps.FOUserAgent;
import org.apache.fop.apps.Fop;
import org.apache.fop.apps.FopFactory;
import org.xml.sax.SAXException;

import ru.idealplm.utils.common.ReportBuilder;

public class PDFBuilder implements ReportBuilder, Runnable {

	private FopFactory fopFactory = null;
	private Transformer transformer = null;
	private OutputStream out = null;
	private Result saxResult = null;
	private InputStream xsltTemplateStream = null;
	private InputStream fontConfigStream = null;
	private File outPdf = null;
	private File xmlFile = null;
	private Object lock;

	public PDFBuilder(InputStream xsltTemplateStream, InputStream fontConfigStream) throws Exception {
		this.xsltTemplateStream = xsltTemplateStream;
		this.fontConfigStream = fontConfigStream;

		if(xsltTemplateStream==null || fontConfigStream==null)
			throw new Exception("Null stream is not allowed.");

		Thread pdfThread = new Thread(this);
		pdfThread.start();
	}

	@Override
	public File getReport() {
		return outPdf;
	}

	@Override
	public void passSourceFile(File xmlFile, Object lock) {
		this.xmlFile = xmlFile;
		this.lock = lock;
		synchronized (this) {
			this.notify();
		}
	}

	@Override
	public void run() {
		try {
			outPdf = File.createTempFile("report", ".pdf");
			DefaultConfigurationBuilder configurationBuilder = new DefaultConfigurationBuilder();
			Configuration configuration = configurationBuilder
					.build(fontConfigStream);
			fopFactory = FopFactory.newInstance();
			fopFactory.setUserConfig(configuration);
			fopFactory.setBaseURL(outPdf.getParentFile().getAbsolutePath()+"\\");
			FOUserAgent foUserAgent = fopFactory.newFOUserAgent();
			foUserAgent
					.getFactory()
					.getFontManager()
					.setFontBaseURL(
							"file:///" + System.getenv("SystemRoot") + "/fonts");
			out = new BufferedOutputStream(new FileOutputStream(outPdf));
			Fop fop = fopFactory.newFop("application/pdf", foUserAgent, out);
			TransformerFactory factory = TransformerFactory.newInstance();
			transformer = factory.newTransformer(new StreamSource(
					xsltTemplateStream));
			transformer.setParameter("versionParam", "1.0");
			saxResult = new SAXResult(fop.getDefaultHandler());
			synchronized (this) {
				this.wait();
			}
			transformer.transform(new StreamSource(xmlFile), saxResult);
			if (out != null)
				out.close();
			if (fopFactory != null)
				fopFactory.getImageManager().getCache().clearCache();
		} catch (IOException e) {
			System.out.println("1");
			e.printStackTrace();
		} catch (ConfigurationException e) {
			System.out.println("2");
			e.printStackTrace();
		} catch (SAXException e) {
			System.out.println("3");
			e.printStackTrace();
		} catch (TransformerConfigurationException e) {
			System.out.println("4");
			e.printStackTrace();
		} catch (InterruptedException e) {
			System.out.println("5");
			e.printStackTrace();
		} catch (TransformerException e) {
			System.out.println("6");
			e.printStackTrace();
		}
		synchronized (lock) {			
			lock.notify();
		}
	}

	public static File xml2pdf(File xmlFile, InputStream xsltStream,
			InputStream fontConfigStream) {

		System.out.println("static xml2pdf module starts...");

		File outPDF = null;
		if (xmlFile != null) {
			String name = xmlFile.getAbsolutePath();
			int point_pos = name.lastIndexOf(".");
			if (point_pos == -1)
				name = name + ".pdf";
			else
				name = name.substring(0, point_pos + 1) + "pdf";
			outPDF = new File(name);
		}

		FopFactory fopFactory = null;
		OutputStream out = null;

		try {
			System.out.println("Static transforming...");

			InputStream configurationStream = fontConfigStream;
			DefaultConfigurationBuilder configurationBuilder = new DefaultConfigurationBuilder();
			Configuration configuration = configurationBuilder
					.build(configurationStream);
			// String sysFontDir = new URI((System.getenv("SystemRoot") +
			// "\\fonts")).normalize().getPath();
			// String sysFontDir = new URI(("file:///" +
			// System.getenv("SystemRoot") + "/fonts")).normalize().getPath();

			// System.out.println("System font directory: " + sysFontDir);

			fopFactory = FopFactory.newInstance();
			fopFactory.setUserConfig(configuration);
			fopFactory.setBaseURL(outPDF.getParentFile().getAbsolutePath()+"\\");
			System.out.println("~~~~~~"+fopFactory.getBaseURL());
			FOUserAgent foUserAgent = fopFactory.newFOUserAgent();
			// foUserAgent.getFactory().getFontManager().setFontBaseURL(sysFontDir);
			foUserAgent
					.getFactory()
					.getFontManager()
					.setFontBaseURL(
							"file:///" + System.getenv("SystemRoot") + "/fonts");

			out = new FileOutputStream(outPDF);
			out = new BufferedOutputStream(out);
			Fop fop = fopFactory.newFop("application/pdf", foUserAgent, out);

			TransformerFactory factory = TransformerFactory.newInstance();
			Transformer transformer = factory.newTransformer(new StreamSource(
					xsltStream));
			transformer.setParameter("versionParam", "1.0");

			Source src = new StreamSource(xmlFile);
			Result res = new SAXResult(fop.getDefaultHandler());
			transformer.transform(src, res);
			if (out != null)
				out.close();
			if (fopFactory != null)
				fopFactory.getImageManager().getCache().clearCache();
		} catch (IOException e) {
			System.out.println("1");
			e.printStackTrace();
		} catch (ConfigurationException e) {
			System.out.println("2");
			e.printStackTrace();
		} catch (SAXException e) {
			System.out.println("3");
			e.printStackTrace();
		} catch (TransformerConfigurationException e) {
			System.out.println("4");
			e.printStackTrace();
		} catch (TransformerException e) {
			System.out.println("6");
			e.printStackTrace();
		}
		System.out.println("static xml2pdf module finished...");
		return outPDF;
	}

}
