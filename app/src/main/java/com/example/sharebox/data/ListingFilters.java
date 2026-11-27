package com.example.sharebox.data;

import java.util.Arrays;

import static com.example.sharebox.data.Models.search;

public final class ListingFilters {
    private ListingFilters() {}

    public enum Condition {
        NEW("Mới"), USED("Đã dùng");

        public final String label;

        Condition(String label) {
            this.label=label;
        }

        public static Condition fromStored(String value) {
            if(value==null)return null;
            switch(search(value)){
                case "moi":
                case "new":return NEW;
                case "da dung":
                case "nhu moi":
                case "used":
                case "like new":return USED;
                default:return null;
            }
        }
    }

    public enum PriceRange {
        BELOW_500K("Dưới 500.000 VND",0,499_999),
        FROM_500K("500.000 – dưới 1.000.000 VND",500_000,999_999),
        FROM_1M("1.000.000 – dưới 2.500.000 VND",1_000_000,2_499_999),
        FROM_2_5M("2.500.000 – 5.000.000 VND",2_500_000,5_000_000),
        ABOVE_5M("Trên 5.000.000 VND",5_000_001,Long.MAX_VALUE);

        public final String label;
        private final long minimum,maximum;

        PriceRange(String label,long minimum,long maximum) {
            this.label=label;
            this.minimum=minimum;
            this.maximum=maximum;
        }

        public boolean contains(long dailyRate) {
            return dailyRate>=minimum&&dailyRate<=maximum;
        }
    }

    public enum Location {
        HO_CHI_MINH("TP. Hồ Chí Minh","TP.HCM","HCM","TPHCM","Sài Gòn","Bình Dương","Dĩ An","Thuận An",
                "Bà Rịa","Vũng Tàu","Thủ Đức","Bình Thạnh","Gò Vấp","Tân Bình","Tân Phú","Bình Tân",
                "Phú Nhuận","Bình Chánh","Hóc Môn","Nhà Bè","Củ Chi","Cần Giờ"),
        HA_NOI("Hà Nội","Hanoi"), DA_NANG("Đà Nẵng","Danang","Quảng Nam"),
        CAN_THO("Cần Thơ","Hậu Giang","Sóc Trăng"),
        HAI_PHONG("Hải Phòng","Haiphong","Hải Dương"), HUE("Huế","Thừa Thiên Huế"),
        AN_GIANG("An Giang","Kiên Giang"), BAC_NINH("Bắc Ninh","Bắc Giang"), CA_MAU("Cà Mau","Bạc Liêu"),
        CAO_BANG("Cao Bằng"), DAK_LAK("Đắk Lắk","Đắc Lắc","Daklak","Phú Yên"), DIEN_BIEN("Điện Biên"),
        DONG_NAI("Đồng Nai","Bình Phước"), DONG_THAP("Đồng Tháp","Tiền Giang"), GIA_LAI("Gia Lai","Bình Định"),
        HA_TINH("Hà Tĩnh"), HUNG_YEN("Hưng Yên","Thái Bình"), KHANH_HOA("Khánh Hòa","Ninh Thuận"),
        LAI_CHAU("Lai Châu"), LAM_DONG("Lâm Đồng","Đắk Nông","Bình Thuận"), LANG_SON("Lạng Sơn"),
        LAO_CAI("Lào Cai","Yên Bái"), NGHE_AN("Nghệ An"), NINH_BINH("Ninh Bình","Hà Nam","Nam Định"),
        PHU_THO("Phú Thọ","Vĩnh Phúc","Hòa Bình"), QUANG_NGAI("Quảng Ngãi","Kon Tum"), QUANG_NINH("Quảng Ninh"),
        QUANG_TRI("Quảng Trị","Quảng Bình"), SON_LA("Sơn La"), TAY_NINH("Tây Ninh","Long An"),
        THAI_NGUYEN("Thái Nguyên","Bắc Kạn"), THANH_HOA("Thanh Hóa"), TUYEN_QUANG("Tuyên Quang","Hà Giang"),
        VINH_LONG("Vĩnh Long","Bến Tre","Trà Vinh");

        public final String label;
        private final String name;
        private final String[] aliases;

        Location(String label,String... aliases) {
            this.label=label;
            this.name=locationWords(label).replaceFirst("^tp ","");
            this.aliases=Arrays.stream(aliases).map(ListingFilters::locationWords).toArray(String[]::new);
        }

        public static Location fromAddress(String address) {
            if(address==null)return null;
            String[] parts=address.split("[,;]");
            for(int i=parts.length-1;i>=0;i--){
                String part=locationWords(parts[i]);
                for(Location location:values()){
                    if(hasWords(part,location.name))return location;
                }
                for(Location location:values()){
                    for(String alias:location.aliases){
                        if(hasWords(part,alias))return location;
                    }
                }
                if(part.matches("quan (?:[1-9]|1[0-2])"))return HO_CHI_MINH;
            }
            return null;
        }

        private static boolean hasWords(String candidate,String name) {
            return (" "+candidate+" ").contains(" "+name+" ");
        }
    }

    private static String locationWords(String value) {
        return search(value).replaceAll("[^a-z0-9]+"," ").trim();
    }
}

