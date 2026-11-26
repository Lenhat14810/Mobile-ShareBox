package com.example.sharebox;

import com.example.sharebox.data.MockShareBoxRepository;
import org.junit.Test;
import java.time.LocalDate;
import java.util.List;
import static com.example.sharebox.data.Models.*;
import static org.junit.Assert.*;

public final class ShareBoxFlowTest {
    private Transaction rental() {
        return new Transaction(20,1,"Trần Quốc Lâm",Role.RENTER,DEMO,3,220000,1000000);
    }

    @Test public void rentSeparatesDaysFeesAndDeposit() {
        Transaction t=rental();
        assertEquals(DEMO.plusDays(2),t.end());
        assertEquals(660000L,t.fee());
        assertEquals(1660000L,t.total());
        assertEquals(0L,t.refund());
    }

    @Test public void paymentRequiresPayerAndPayee() {
        Transaction t=rental();
        assertFalse(t.confirm(Step.ACCEPT,Role.RENTER));
        assertTrue(t.confirm(Step.ACCEPT,Role.OWNER));
        assertEquals(Stage.PAYMENT,t.stage());
        assertFalse(t.confirm(Step.RECEIVED_MONEY,Role.OWNER));
        assertTrue(t.confirm(Step.PAID,Role.RENTER));
        assertFalse(t.confirm(Step.PAID,Role.RENTER));
        assertEquals(Stage.PAYMENT,t.stage());
        assertTrue(t.confirm(Step.RECEIVED_MONEY,Role.OWNER));
        assertEquals(Stage.HANDOVER,t.stage());
    }

    @Test public void handoverAndReturnWaitForBothSides() {
        Transaction t=rental();
        for(Step s:List.of(Step.ACCEPT,Step.PAID,Step.RECEIVED_MONEY)) assertTrue(t.confirm(s,s.role));
        assertTrue(t.confirm(Step.RECEIVED_ITEM,Role.RENTER));
        assertEquals(Stage.HANDOVER,t.stage());
        assertTrue(t.confirm(Step.HANDED,Role.OWNER));
        assertEquals(Stage.ACTIVE,t.stage());
        assertTrue(t.confirm(Step.RETURNED,Role.RENTER));
        assertEquals(Stage.RETURN,t.stage());
        assertEquals(0L,t.refund());
        t.deduction=150000;
        assertTrue(t.confirm(Step.RECEIVED_BACK,Role.OWNER));
        assertEquals(Stage.COMPLETE,t.stage());
        assertEquals(850000L,t.refund());
    }

    @Test public void disputePausesProgressAndRestoresUnderlyingStage() {
        Transaction t=rental();
        for(Step s:List.of(Step.ACCEPT,Step.PAID,Step.RECEIVED_MONEY,Step.HANDED,Step.RECEIVED_ITEM,Step.RETURNED)) assertTrue(t.confirm(s,s.role));
        t.dispute("Kiểm tra tình trạng máy.");
        assertEquals("Tranh chấp",t.label());
        assertEquals(Stage.RETURN,t.stage());
        assertTrue(t.actions().isEmpty());
        assertFalse(t.confirm(Step.RECEIVED_BACK,Role.OWNER));
        assertEquals(0L,t.refund());
        t.clearDispute();
        assertTrue(t.confirm(Step.RECEIVED_BACK,Role.OWNER));
        assertEquals(Stage.COMPLETE,t.stage());
        t.dispute("Không thể mở lại giao dịch đã hoàn tất.");
        assertFalse(t.disputed());
    }

    @Test public void cancellationIsTerminal() {
        Transaction t=rental();
        t.cancel();
        assertEquals(Stage.CANCELLED,t.stage());
        assertTrue(t.actions().isEmpty());
        assertFalse(t.confirm(Step.ACCEPT,Role.OWNER));
    }

    @Test public void calendarRejectsUnavailableDatesAndInvalidDuration() {
        State s=new MockShareBoxRepository().demoState();
        assertTrue(s.available(1,DEMO,3));
        assertFalse(s.available(1,DEMO,7));
        assertFalse(s.available(1,DEMO,15));
        assertFalse(s.available(1,DEMO,0));
    }

    @Test public void seedMaintainsVietnameseSearchAndTwoUsageTypes() {
        State s=new MockShareBoxRepository().demoState();
        assertEquals("may anh canon eos 200d",search(s.product(1).name));
        assertEquals("Miễn phí",s.product(2).price());
        assertEquals(Usage.BORROW,s.product(2).usage());
        assertEquals(Usage.RENT,s.product(1).usage());
        assertEquals("250.000đ/ngày",s.product(1).price());
    }

    @Test public void financeSeedSeparatesDepositsFromOperatingIncome() {
        State s=new MockShareBoxRepository().demoState();
        LocalDate month=DEMO.withDayOfMonth(1);
        List<Entry> entries=s.entries.stream().filter(e->!e.date.isBefore(month))
                .collect(java.util.stream.Collectors.toList());
        assertEquals(4820000L,entries.stream().filter(e->e.type==EntryType.INCOME).mapToLong(e->e.amount).sum());
        assertEquals(-1660000L,entries.stream().filter(e->e.type==EntryType.EXPENSE).mapToLong(e->e.amount).sum());
        assertEquals(1200000L,entries.stream().filter(e->e.type==EntryType.HELD).mapToLong(e->e.amount).sum());
        assertTrue(EntryType.REFUND.deposit());
        assertTrue(EntryType.DEDUCTION.deposit());
        assertFalse(EntryType.INCOME.deposit());
    }
}
