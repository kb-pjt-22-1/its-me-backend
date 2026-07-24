package site.benepay.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.oned.Code128Writer;
import com.google.zxing.qrcode.QRCodeWriter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

public final class QrCodeUtil {
    private QrCodeUtil() {
    }

    public static String qrDataUrl(String content) {
        return toDataUrl(createQr(content, 320, 320));
    }

    public static String barcodeDataUrl(String content) {
        return toDataUrl(createBarcode(content, 720, 180));
    }

    private static byte[] createQr(String content, int width, int height) {
        try {
            BitMatrix matrix = new QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, width, height);
            return writePng(matrix);
        } catch (WriterException e) {
            throw new IllegalStateException("QR code generation failed", e);
        }
    }

    private static byte[] createBarcode(String content, int width, int height) {
        try {
            BitMatrix matrix = new Code128Writer().encode(content, BarcodeFormat.CODE_128, width, height);
            return writePng(matrix);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Barcode generation failed", e);
        }
    }

    private static byte[] writePng(BitMatrix matrix) {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            MatrixToImageWriter.writeToStream(matrix, "PNG", output);
            return output.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Image encoding failed", e);
        }
    }

    private static String toDataUrl(byte[] imageBytes) {
        return "data:image/png;base64," + Base64.getEncoder().encodeToString(imageBytes);
    }
}
