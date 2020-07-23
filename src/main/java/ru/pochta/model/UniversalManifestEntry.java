package ru.pochta.model;

import com.opencsv.bean.CsvBindByName;
import lombok.*;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Builder
@Getter
@Setter
@Entity(name = "universal_manifest_entries")
@NoArgsConstructor
@AllArgsConstructor
public class UniversalManifestEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @CsvBindByName(column = "SellerID")
    private String sellerID;
    @CsvBindByName(column = "ItemTrackingNumber")
    private String itemTrackingNumber;
    @CsvBindByName(column = "Weight")
    private String weight;
    @CsvBindByName(column = "Price")
    private String price;
    @CsvBindByName(column = "PriceCurrency")
    private String priceCurrency;
    @CsvBindByName(column = "Product")
    private String product;
    @CsvBindByName(column = "ReceptacleID")
    private String receptacleID;
    @CsvBindByName(column = "SenderName")
    private String senderName;
    @CsvBindByName(column = "SenderCountry")
    private String senderCountry;
    @CsvBindByName(column = "SenderZip")
    private String senderZip;
    @CsvBindByName(column = "SenderLocality")
    private String senderLocality;
    @CsvBindByName(column = "SenderAddress")
    private String senderAddress;
    @CsvBindByName(column = "ReceiverName")
    private String receiverName;
    @CsvBindByName(column = "ReceiverCountry")
    private String receiverCountry;
    @CsvBindByName(column = "ReceiverZip")
    private String receiverZip;
    @CsvBindByName(column = "ReceiverAddress")
    private String receiverAddress;
    @CsvBindByName(column = "ReceiverPhone")
    private String receiverPhone;
    @CsvBindByName(column = "ReceiverEmail")
    private String receiverEmail;
    @CsvBindByName(column = "OrderNo")
    private String orderNo;
    @CsvBindByName(column = "OrderDateTime")
    private String orderDateTime;
    @CsvBindByName(column = "LogisticsOrderCode")
    private String logisticsOrderCode;

}
