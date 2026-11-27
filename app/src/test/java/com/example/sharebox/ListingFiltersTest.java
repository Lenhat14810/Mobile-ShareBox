package com.example.sharebox;

import com.example.sharebox.data.ListingFilters.Condition;
import com.example.sharebox.data.ListingFilters.Location;
import com.example.sharebox.data.ListingFilters.PriceRange;
import org.junit.Test;

import static org.junit.Assert.*;

public final class ListingFiltersTest {
    @Test public void priceRangesPartitionDailyVndRatesAtEveryBoundary() {
        long[] boundaries={0,499_999,500_000,999_999,1_000_000,2_499_999,2_500_000,5_000_000,5_000_001,Long.MAX_VALUE};
        PriceRange[] ranges=PriceRange.values();
        for(int i=0;i<boundaries.length;i++){
            for(PriceRange range:ranges)assertEquals(ranges[i/2]==range,range.contains(boundaries[i]));
        }
        for(PriceRange range:ranges)assertFalse(range.contains(-1));
    }

    @Test public void locationsHandleExplicitAddressesAndOlderPlaceNames(){
        assertEquals(34,Location.values().length);
        assertEquals(Location.HO_CHI_MINH,Location.fromAddress("Đường Hà Nội, TP.HCM"));
        assertEquals(Location.HO_CHI_MINH,Location.fromAddress("Quận 7"));
        assertEquals(Location.HO_CHI_MINH,Location.fromAddress("Dĩ An, Bình Dương"));
        assertEquals(Location.HA_NOI,Location.fromAddress("Cầu Giấy, Ha Noi"));
        assertEquals(Location.DA_NANG,Location.fromAddress("Hội An, Quảng Nam"));
        assertEquals(Location.CAN_THO,Location.fromAddress("Ninh Kiều, Cần Thơ"));
        assertEquals(Location.HAI_PHONG,Location.fromAddress("Hải Dương"));
        assertNull(Location.fromAddress("Không rõ"));
    }

    @Test public void conditionsAcceptCanonicalAndLegacyValues(){
        assertArrayEquals(new Condition[]{Condition.NEW,Condition.USED},Condition.values());
        assertEquals(Condition.NEW,Condition.fromStored("Mới"));
        assertEquals(Condition.USED,Condition.fromStored("Đã dùng"));
        assertEquals(Condition.USED,Condition.fromStored("Như mới"));
        assertNull(Condition.fromStored("Không rõ"));
        assertNull(Condition.fromStored(null));
    }
}
