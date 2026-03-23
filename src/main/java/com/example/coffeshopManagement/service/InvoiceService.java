package com.example.coffeshopManagement.service;

import com.example.coffeshopManagement.entity.OrderItem;
import com.example.coffeshopManagement.entity.Payment;
import com.example.coffeshopManagement.entity.ShopOrder;
import com.example.coffeshopManagement.exception.BadRequestException;
import com.example.coffeshopManagement.exception.ResourceNotFoundException;
import com.example.coffeshopManagement.repository.OrderItemRepository;
import com.example.coffeshopManagement.repository.PaymentRepository;
import com.example.coffeshopManagement.repository.ShopOrderRepository;
import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

@Service
public class InvoiceService {
    private final ShopOrderRepository shopOrderRepository;
    private final OrderItemRepository orderItemRepository;
    private final PaymentRepository paymentRepository;

    public InvoiceService(
            ShopOrderRepository shopOrderRepository,
            OrderItemRepository orderItemRepository,
            PaymentRepository paymentRepository) {
        this.shopOrderRepository = shopOrderRepository;
        this.orderItemRepository = orderItemRepository;
        this.paymentRepository = paymentRepository;
    }

    public byte[] generateInvoicePdf(Integer orderId) {
        ShopOrder order = shopOrderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new BadRequestException("Order is not paid yet"));
        List<OrderItem> items = orderItemRepository.findByOrderId(orderId);

        NumberFormat vnd = NumberFormat.getNumberInstance(new Locale("vi", "VN"));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document();
        PdfWriter.getInstance(document, out);
        document.open();

        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
        Font normal = FontFactory.getFont(FontFactory.HELVETICA, 11);
        document.add(new Paragraph("COFFEE SHOP INVOICE", titleFont));
        document.add(new Paragraph("Order ID: #" + order.getId(), normal));
        document.add(new Paragraph("Table: " + order.getTable().getName(), normal));
        document.add(new Paragraph("Cashier: " + order.getUser().getUsername(), normal));
        document.add(new Paragraph("Payment: " + payment.getMethod().name(), normal));
        document.add(new Paragraph(" ", normal));

        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.addCell("Item");
        table.addCell("Qty");
        table.addCell("Unit Price");
        table.addCell("Line Total");

        for (OrderItem item : items) {
            table.addCell(item.getMenuItem().getName());
            table.addCell(String.valueOf(item.getQuantity()));
            table.addCell(vnd.format(item.getUnitPrice()) + " VND");
            table.addCell(vnd.format(item.getUnitPrice().multiply(java.math.BigDecimal.valueOf(item.getQuantity()))) + " VND");
        }
        document.add(table);
        document.add(new Paragraph(" ", normal));
        document.add(new Paragraph("Subtotal: " + vnd.format(order.getSubtotal()) + " VND", normal));
        document.add(new Paragraph("Discount: " + vnd.format(order.getDiscount()) + " VND", normal));
        document.add(new Paragraph("Total: " + vnd.format(order.getTotal()) + " VND", titleFont));
        document.close();

        return out.toByteArray();
    }
}
