package com.b4rrhh.geo.address.infrastructure.persistence;

import com.b4rrhh.geo.address.domain.model.PostalAddress;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import org.hibernate.annotations.Formula;

/**
 * Las columnas de una dirección, las mismas en {@code employee.address},
 * {@code rulesystem.company_profile} y {@code rulesystem.work_center_profile} (V177). Las tres
 * entidades JPA la embeben, así que el mapeo se escribe una vez; que el esquema diga lo mismo lo
 * comprueba {@code AddressStructureIsTheSameInTheThreeTablesTest}.
 *
 * <p>{@code municipalityName} no es una columna: se lee del maestro {@code geo} al cargar la fila.</p>
 */
@Embeddable
public class PostalAddressColumns {

    @Column(name = "street_type_code", length = 30)
    private String streetTypeCode;

    @Column(name = "street_name", length = 200)
    private String streetName;

    @Column(name = "street_number", length = 10)
    private String streetNumber;

    @Column(name = "number_qualifier", length = 10)
    private String numberQualifier;

    @Column(name = "block", length = 10)
    private String block;

    @Column(name = "stair", length = 10)
    private String stair;

    @Column(name = "floor", length = 10)
    private String floor;

    @Column(name = "door", length = 10)
    private String door;

    @Column(name = "locality", length = 120)
    private String locality;

    @Column(name = "municipality_code", length = 10)
    private String municipalityCode;

    @Column(name = "subdivision_code", length = 10)
    private String subdivisionCode;

    @Column(name = "postal_code", length = 20)
    private String postalCode;

    @Column(name = "country_code", length = 3)
    private String countryCode;

    @Column(name = "normalized", nullable = false)
    private boolean normalized;

    @Formula("(select m.name from geo.municipality m where m.country_code = 'ESP' and m.code = municipality_code)")
    private String municipalityName;

    public static PostalAddressColumns of(PostalAddress address) {
        PostalAddressColumns columns = new PostalAddressColumns();
        if (address == null) {
            return columns;
        }
        columns.streetTypeCode = address.streetTypeCode();
        columns.streetName = address.streetName();
        columns.streetNumber = address.streetNumber();
        columns.numberQualifier = address.numberQualifier();
        columns.block = address.block();
        columns.stair = address.stair();
        columns.floor = address.floor();
        columns.door = address.door();
        columns.locality = address.locality();
        columns.municipalityCode = address.municipalityCode();
        columns.subdivisionCode = address.subdivisionCode();
        columns.postalCode = address.postalCode();
        columns.countryCode = address.countryCode();
        columns.normalized = address.normalized();
        columns.municipalityName = address.municipalityName();
        return columns;
    }

    /** La dirección, o {@code null} si la fila no tiene ninguna: un perfil sin dirección (backend#155). */
    public PostalAddress toDomainOrNull() {
        boolean empty = streetTypeCode == null && streetName == null && streetNumber == null && numberQualifier == null
                && block == null && stair == null && floor == null && door == null && locality == null
                && municipalityCode == null && subdivisionCode == null && postalCode == null && countryCode == null;
        return empty ? null : toDomain();
    }

    public PostalAddress toDomain() {
        return new PostalAddress(streetTypeCode, streetName, streetNumber, numberQualifier, block, stair,
                floor, door, locality, municipalityCode, subdivisionCode, postalCode, countryCode, normalized,
                municipalityName);
    }
}
