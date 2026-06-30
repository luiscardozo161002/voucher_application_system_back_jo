package mx.juarezdeoriente.solicitudes.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Datos institucionales que aparecen en el PDF del pedido.
 * Configurables vía variables de entorno COMPANY_*.
 * Registrado via @EnableConfigurationProperties en SolicitudesApplication.
 */
@ConfigurationProperties(prefix = "app.company")
public class CompanyProperties {

    private String name;
    private String address;
    private String cityLine;
    private String postalCode;
    private String rfc;
    private String phone;
    private String extension;
    private String documentCode;
    private List<String> authorizers;

    public String getName()         { return name; }
    public void setName(String v)   { this.name = v; }

    public String getAddress()           { return address; }
    public void setAddress(String v)     { this.address = v; }

    public String getCityLine()          { return cityLine; }
    public void setCityLine(String v)    { this.cityLine = v; }

    public String getPostalCode()        { return postalCode; }
    public void setPostalCode(String v)  { this.postalCode = v; }

    public String getRfc()           { return rfc; }
    public void setRfc(String v)     { this.rfc = v; }

    public String getPhone()         { return phone; }
    public void setPhone(String v)   { this.phone = v; }

    public String getExtension()     { return extension; }
    public void setExtension(String v){ this.extension = v; }

    public String getDocumentCode()       { return documentCode; }
    public void setDocumentCode(String v) { this.documentCode = v; }

    public List<String> getAuthorizers()       { return authorizers; }
    public void setAuthorizers(List<String> v) { this.authorizers = v; }
}
