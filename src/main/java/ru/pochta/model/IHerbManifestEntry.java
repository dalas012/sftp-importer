package ru.pochta.model;

import com.opencsv.bean.CsvBindByName;
import lombok.*;

import javax.persistence.*;

@Builder
@Getter
@Setter
@Entity(name = "iherb_manifest_entries")
@NoArgsConstructor
@AllArgsConstructor
public class IHerbManifestEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @CsvBindByName(column = "Company name")
    private String companyName;
    @CsvBindByName(column = "№ agreement")
    private String agreementNumber;
    @CsvBindByName(column = "barcode")
    private String barcode;
    @CsvBindByName(column = "sender name")
    private String senderName;
    @CsvBindByName(column = "receiver_name")
    private String receiverName;
    @CsvBindByName(column = "zip_code")
    private String zipCode;
    @CsvBindByName(column = "region")
    private String region;
    @CsvBindByName(column = "district")
    private String district;
    @CsvBindByName(column = "city")
    private String city;
    @CsvBindByName(column = "street")
    private String street;
    @CsvBindByName(column = "house")
    private String house;
    @CsvBindByName(column = "building")
    private String building;
    @CsvBindByName(column = "apartment")
    private String apartment;
    @CsvBindByName(column = "mobile phone number")
    private String mobilePhoneNumber;
    @CsvBindByName(column = "tarif_euro")
    private String tariffEuro;
    @CsvBindByName(column = "tarif_euro_cent")
    private String tariffEuroCent;
    @CsvBindByName(column = "weight_of_the_parcel_kg")
    private int weightOfTheParcelKg;
    @CsvBindByName(column = "weight_of_the_parcel_g")
    private int weightOfTheParcelG;
    @CsvBindByName(column = "type_of_service_code")
    private String typeOfServiceCode;
    @CsvBindByName(column = "COD_amount_kur")
    private String codAmountKur;
    @CsvBindByName(column = "COD_amount_rur_kopeks")
    private String codAmountRurKopeks;
    @CsvBindByName(column = "no_of_the_product")
    private String noOfTheProduct;
    @CsvBindByName(column = "name_of_the_product")
    private String nameOfTheProduct;
    @CsvBindByName(column = "quantity of identical items_of_product")
    private String quantityOfIdenticalItemsOfProduct;
    @CsvBindByName(column = "weight_of_the_product_kg")
    private String weightOfTheProductKg;
    @CsvBindByName(column = "weight_of_the_product_g")
    private String weightOfTheProductG;
    @CsvBindByName(column = "product_value_US")
    private String productValueUS;
    @CsvBindByName(column = "product_value_US_cent")
    private String productValueUSCent;
    @CsvBindByName(column = "category")
    private String category;
    @CsvBindByName(column = "comments")
    private String comments;
    @CsvBindByName(column = "invoice_id")
    private String invoiceId;
    @CsvBindByName(column = "country")
    private String country;
    @CsvBindByName(column = "zip_sender")
    private String zipSender;
    @CsvBindByName(column = "region_sender")
    private String regionSender;
    @CsvBindByName(column = "district1")
    private String district1;
    @CsvBindByName(column = "city_sender")
    private String citySender;
    @CsvBindByName(column = "street_sender")
    private String streetSender;
    @CsvBindByName(column = "bldg_sender")
    private String bldgSender;
    @CsvBindByName(column = "apartment_sender")
    private String apartmentSender;
    @CsvBindByName(column = "*")
    private String asteriskColumn;
    @CsvBindByName(column = "№ of delivery lot")
    private String numberOfDeliveryLot;
    @CsvBindByName(column = "type_delivery_code")
    private String typeDeliveryCode;
    @CsvBindByName(column = "LogisticsOrderCode")
    private String logisticsOrderCode;
    @CsvBindByName(column = "OrderMade")
    private String orderMade;
    @CsvBindByName(column = "IMID")
    private String imid;
    @CsvBindByName(column = "PriceCurrency")
    private String priceCurrency;
    @CsvBindByName(column = "URL of the Internet store")
    private String urlOfTheInternetStore;
    @CsvBindByName(column = "Name of the Internet store")
    private String nameOfTheInternetStore;
    @CsvBindByName(column = "CategoryFeature")
    private String categoryFeature;
    @CsvBindByName(column = "URL from the product")
    private String urlFromTheProduct;
    @CsvBindByName(column = "HSCode")
    private String hsCode;
    @CsvBindByName(column = "ID")
    private String idColumn;
    @CsvBindByName(column = "PickUp_Name")
    private String pickUpName;
    @CsvBindByName(column = "PickUp_ZipCode")
    private String pickUpZipCode;
    @CsvBindByName(column = "Dispatch barcode")
    private String dispatchBarcode;
    @CsvBindByName(column = "Date of dispatch")
    private String dateOfDispatch;
    @CsvBindByName(column = "Airport of Departure")
    private String airportOfDeparture;
    @CsvBindByName(column = "Airport of Arrival")
    private String airportOfArrival;
    @CsvBindByName(column = "No Returns")
    private String noReturns;

}
