package com.guesshole.entities;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.util.Objects;

@Table("location_points")
public class LocationPoint {
    @Id
    private Long id;

    @Column("latitude")
    private Double latitude;

    @Column("longitude")
    private Double longitude;

    // Administrative level 0 (Country)
    @Column("admin0_type")
    private String admin0Type;

    @Column("admin0_name")
    private String admin0Name;

    @Column("gid0")
    private String gid0;

    // Administrative level 1 (State/Province)
    @Column("admin1_type")
    private String admin1Type;

    @Column("admin1_name")
    private String admin1Name;

    @Column("gid1")
    private String gid1;

    // Administrative level 2 (County/District)
    @Column("admin2_type")
    private String admin2Type;

    @Column("admin2_name")
    private String admin2Name;

    @Column("gid2")
    private String gid2;

    // Administrative level 3
    @Column("admin3_type")
    private String admin3Type;

    @Column("admin3_name")
    private String admin3Name;

    @Column("gid3")
    private String gid3;

    // Administrative level 4
    @Column("admin4_type")
    private String admin4Type;

    @Column("admin4_name")
    private String admin4Name;

    @Column("gid4")
    private String gid4;

    // Administrative level 5
    @Column("admin5_type")
    private String admin5Type;

    @Column("admin5_name")
    private String admin5Name;

    @Column("gid5")
    private String gid5;

    // Default constructor needed for Spring Data
    public LocationPoint() {
    }

    // Builder pattern constructor for easier instantiation
    private LocationPoint(Builder builder) {
        this.id = builder.id;
        this.latitude = builder.latitude;
        this.longitude = builder.longitude;
        this.admin0Type = builder.admin0Type;
        this.admin0Name = builder.admin0Name;
        this.gid0 = builder.gid0;
        this.admin1Type = builder.admin1Type;
        this.admin1Name = builder.admin1Name;
        this.gid1 = builder.gid1;
        this.admin2Type = builder.admin2Type;
        this.admin2Name = builder.admin2Name;
        this.gid2 = builder.gid2;
        this.admin3Type = builder.admin3Type;
        this.admin3Name = builder.admin3Name;
        this.gid3 = builder.gid3;
        this.admin4Type = builder.admin4Type;
        this.admin4Name = builder.admin4Name;
        this.gid4 = builder.gid4;
        this.admin5Type = builder.admin5Type;
        this.admin5Name = builder.admin5Name;
        this.gid5 = builder.gid5;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public String getAdmin0Type() {
        return admin0Type;
    }

    public void setAdmin0Type(String admin0Type) {
        this.admin0Type = admin0Type;
    }

    public String getAdmin0Name() {
        return admin0Name;
    }

    public void setAdmin0Name(String admin0Name) {
        this.admin0Name = admin0Name;
    }

    public String getGid0() {
        return gid0;
    }

    public void setGid0(String gid0) {
        this.gid0 = gid0;
    }

    public String getAdmin1Type() {
        return admin1Type;
    }

    public void setAdmin1Type(String admin1Type) {
        this.admin1Type = admin1Type;
    }

    public String getAdmin1Name() {
        return admin1Name;
    }

    public void setAdmin1Name(String admin1Name) {
        this.admin1Name = admin1Name;
    }

    public String getGid1() {
        return gid1;
    }

    public void setGid1(String gid1) {
        this.gid1 = gid1;
    }

    public String getAdmin2Type() {
        return admin2Type;
    }

    public void setAdmin2Type(String admin2Type) {
        this.admin2Type = admin2Type;
    }

    public String getAdmin2Name() {
        return admin2Name;
    }

    public void setAdmin2Name(String admin2Name) {
        this.admin2Name = admin2Name;
    }

    public String getGid2() {
        return gid2;
    }

    public void setGid2(String gid2) {
        this.gid2 = gid2;
    }

    public String getAdmin3Type() {
        return admin3Type;
    }

    public void setAdmin3Type(String admin3Type) {
        this.admin3Type = admin3Type;
    }

    public String getAdmin3Name() {
        return admin3Name;
    }

    public void setAdmin3Name(String admin3Name) {
        this.admin3Name = admin3Name;
    }

    public String getGid3() {
        return gid3;
    }

    public void setGid3(String gid3) {
        this.gid3 = gid3;
    }

    public String getAdmin4Type() {
        return admin4Type;
    }

    public void setAdmin4Type(String admin4Type) {
        this.admin4Type = admin4Type;
    }

    public String getAdmin4Name() {
        return admin4Name;
    }

    public void setAdmin4Name(String admin4Name) {
        this.admin4Name = admin4Name;
    }

    public String getGid4() {
        return gid4;
    }

    public void setGid4(String gid4) {
        this.gid4 = gid4;
    }

    public String getAdmin5Type() {
        return admin5Type;
    }

    public void setAdmin5Type(String admin5Type) {
        this.admin5Type = admin5Type;
    }

    public String getAdmin5Name() {
        return admin5Name;
    }

    public void setAdmin5Name(String admin5Name) {
        this.admin5Name = admin5Name;
    }

    public String getGid5() {
        return gid5;
    }

    public void setGid5(String gid5) {
        this.gid5 = gid5;
    }

    /**
     * Gets the most specific location name available
     * @return The most detailed location name
     */
    public String getMostSpecificName() {
        if (admin5Name != null && !admin5Name.isEmpty()) {
            return admin5Name;
        } else if (admin4Name != null && !admin4Name.isEmpty()) {
            return admin4Name;
        } else if (admin3Name != null && !admin3Name.isEmpty()) {
            return admin3Name;
        } else if (admin2Name != null && !admin2Name.isEmpty()) {
            return admin2Name;
        } else if (admin1Name != null && !admin1Name.isEmpty()) {
            return admin1Name;
        } else {
            return admin0Name;
        }
    }

    /**
     * Gets a formatted representation of the location's administrative hierarchy
     * @return String with format like "City, State, Country"
     */
    public String getFormattedLocation() {
        StringBuilder sb = new StringBuilder();

        // Add the most specific location first
        if (admin5Name != null && !admin5Name.isEmpty()) {
            sb.append(admin5Name);
        } else if (admin4Name != null && !admin4Name.isEmpty()) {
            sb.append(admin4Name);
        } else if (admin3Name != null && !admin3Name.isEmpty()) {
            sb.append(admin3Name);
        } else if (admin2Name != null && !admin2Name.isEmpty()) {
            sb.append(admin2Name);
        }

        // Add state/province if available
        if (admin1Name != null && !admin1Name.isEmpty()) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(admin1Name);
        }

        // Always add country
        if (admin0Name != null && !admin0Name.isEmpty()) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(admin0Name);
        }

        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LocationPoint that = (LocationPoint) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(latitude, that.latitude) &&
                Objects.equals(longitude, that.longitude);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, latitude, longitude);
    }

    @Override
    public String toString() {
        return "LocationPoint{" +
                "id=" + id +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                ", location='" + getFormattedLocation() + '\'' +
                '}';
    }

    // Builder pattern for easier construction
    public static class Builder {
        private Long id;
        private Double latitude;
        private Double longitude;
        private String admin0Type;
        private String admin0Name;
        private String gid0;
        private String admin1Type;
        private String admin1Name;
        private String gid1;
        private String admin2Type;
        private String admin2Name;
        private String gid2;
        private String admin3Type;
        private String admin3Name;
        private String gid3;
        private String admin4Type;
        private String admin4Name;
        private String gid4;
        private String admin5Type;
        private String admin5Name;
        private String gid5;

        public Builder() {
        }

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder coordinates(Double latitude, Double longitude) {
            this.latitude = latitude;
            this.longitude = longitude;
            return this;
        }

        public Builder level0(String type, String name, String gid) {
            this.admin0Type = type;
            this.admin0Name = name;
            this.gid0 = gid;
            return this;
        }

        public Builder level1(String type, String name, String gid) {
            this.admin1Type = type;
            this.admin1Name = name;
            this.gid1 = gid;
            return this;
        }

        public Builder level2(String type, String name, String gid) {
            this.admin2Type = type;
            this.admin2Name = name;
            this.gid2 = gid;
            return this;
        }

        public Builder level3(String type, String name, String gid) {
            this.admin3Type = type;
            this.admin3Name = name;
            this.gid3 = gid;
            return this;
        }

        public Builder level4(String type, String name, String gid) {
            this.admin4Type = type;
            this.admin4Name = name;
            this.gid4 = gid;
            return this;
        }

        public Builder level5(String type, String name, String gid) {
            this.admin5Type = type;
            this.admin5Name = name;
            this.gid5 = gid;
            return this;
        }

        public LocationPoint build() {
            return new LocationPoint(this);
        }
    }
}