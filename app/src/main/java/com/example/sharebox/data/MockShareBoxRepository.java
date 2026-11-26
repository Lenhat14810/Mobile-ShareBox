package com.example.sharebox.data;

import java.time.LocalDate;
import java.util.*;
import static com.example.sharebox.data.Models.*;

public final class MockShareBoxRepository implements ShareBoxRepository {
    @Override public State demoState() {
        State s = new State();
        add(s,1,"Máy ảnh Canon EOS 200D",Art.CAMERA,250000,1000000,"Thủ Đức",2,"Trần Quốc Lâm","Điện tử","Máy mua năm 2022, ít dùng, màn hình xoay lật. Kèm pin, sạc, thẻ nhớ 64GB và túi đựng.",false,Status.ACTIVE,4,38,52,"");
        add(s,2,"Máy khoan Bosch GSB 550",Art.DRILL,0,200000,"Quận 7",3,"Phạm Thu Hà","Đồ gia dụng","Máy khoan có bộ mũi khoan đi kèm.",true,Status.ACTIVE,5,25,34,"Đang cho mượn đến 11/12");
        add(s,3,"Laptop Dell Inspiron 15",Art.LAPTOP,70000,1500000,"Quận 1",1,"Lê Hoàng Nam","Laptop","Laptop văn phòng, có sạc đi kèm.",false,Status.ACTIVE,2,4,8,"");
        add(s,4,"Ghế gaming E-Dra",Art.CHAIR,150000,0,"Bình Thạnh",2,"Võ Ngọc Trâm","Đồ gia dụng","Ghế có thể chỉnh độ cao và góc tựa.",false,Status.ACTIVE,6,1,5,"");
        add(s,5,"Loa JBL Charge 4",Art.SPEAKER,80000,300000,"Quận 10",1,"Nguyễn Minh Anh","Điện tử","Loa Bluetooth, có sạc đi kèm.",true,Status.ACTIVE,1,8,12,"Đã có 1 yêu cầu thuê");
        add(s,6,"Giáo trình Giải tích 1",Art.BOOK,0,0,"Dĩ An",5,"Nguyễn Minh Anh","Sách","Giáo trình có ghi chú ở một số trang.",true,Status.HIDDEN,7,26,38,"Đang ẩn khỏi tìm kiếm");
        add(s,7,"Laptop Acer Aspire 5",Art.LAPTOP,60000,1000000,"Thủ Đức",1,"Hà My","Laptop","Laptop phù hợp học tập và làm việc.",false,Status.ACTIVE,6,10,12,"");
        add(s,8,"Xe đạp địa hình Giant",Art.BIKE,90000,400000,"Dĩ An",1,"Đặng Minh Khôi","Thể thao","Xe đạp địa hình, đã bảo dưỡng.",false,Status.HIDDEN,9,5,10,"");
        add(s,9,"Máy chiếu mini Xiaomi",Art.LAPTOP,380000,500000,"Thủ Đức",1,"Nguyễn Minh Anh","Điện tử","Máy chiếu nhỏ gọn, có dây nguồn.",true,Status.ACTIVE,1,9,12,"Trống đến hết 31/12");
        add(s,10,"Đèn bàn học",Art.BOOK,30000,0,"Thủ Đức",1,"Nguyễn Minh Anh","Đồ gia dụng","Đèn bàn LED.",true,Status.REMOVED,7,0,0,"Tin đã gỡ");
        add(s,11,"Bàn gấp học tập",Art.BOOK,30000,0,"Thủ Đức",1,"Nguyễn Minh Anh","Đồ gia dụng","Bàn gấp nhỏ gọn dùng để học và làm việc.",true,Status.HIDDEN,6,0,0,"Đang ẩn khỏi tìm kiếm");
        s.favorites.addAll(Arrays.asList(1,5,6,3,4));
        s.chats.put(1,new Conversation(1,"Trần Quốc Lâm","TL",2,
                Message.text("Chào bạn, máy ảnh Canon vẫn còn trống nhé. Bạn định thuê mấy ngày?",false,"09:58"),
                Message.text("Mình thuê 3 ngày từ 8/12. Thuê 3 ngày thì giảm được không ạ?",true,"10:00"),
                Message.text("Thuê 3 ngày mình tính 220.000đ/ngày, cọc 1.000.000đ, trả máy xong hoàn cọc ngay.",false,"10:01"),
                new Message("Ảnh máy thực tế",false,"10:02",null,Art.CAMERA,false),
                new Message("Mã QR minh họa",false,"10:02",null,null,true)));
        chat(s,2,"Mình qua lấy lúc 5h chiều nhé","09:41",1);
        chat(s,3,"Đã tạo yêu cầu thuê – chờ xác nhận","Hôm qua",0);
        chat(s,4,"Cảm ơn bạn, mình đã trả ghế","Thứ 6",0);
        chat(s,8,"Cuối tuần này xe còn trống chứ?","Thứ 4",0);
        tx(s,1,1,220000,1000000,3,Role.RENTER,0);
        tx(s,2,5,80000,300000,2,Role.OWNER,0);
        tx(s,3,2,0,200000,4,Role.RENTER,5);
        tx(s,4,3,70000,1500000,3,Role.RENTER,3);
        tx(s,5,1,220000,1000000,3,Role.RENTER,3);
        tx(s,6,8,90000,400000,2,Role.RENTER,1);
        tx(s,7,4,150000,0,2,Role.RENTER,7);
        tx(s,8,9,380000,1000000,1,Role.OWNER,7);
        s.notices.add(new Notice(1,NoticeType.CHAT,"Trần Quốc Lâm đã nhắn tin","Thuê 3 ngày mình tính 220.000đ/ngày, cọc 1.000.000đ nhé.","10:01",1,false));
        s.notices.add(new Notice(2,NoticeType.REQUEST,"Yêu cầu thuê mới","Bùi Thảo muốn thuê Loa JBL Charge 4 trong 2 ngày.","09:30",2,false));
        s.notices.add(new Notice(3,NoticeType.PAYMENT,"Chủ đồ đã xác nhận nhận tiền","Thanh toán thuê Máy ảnh Canon EOS 200D đã được xác nhận.","08:45",5,false));
        s.notices.add(new Notice(4,NoticeType.REMINDER,"Nhắc trả đồ","Máy khoan Bosch cần được trả vào ngày 11/12.","Hôm qua",3,true));
        s.notices.add(new Notice(5,NoticeType.STATUS,"Giao dịch hoàn tất","Thuê Ghế gaming E-Dra đã hoàn tất.","Thứ 6",7,true));
        s.notices.add(new Notice(6,NoticeType.CHAT,"Phạm Thu Hà đã nhắn tin","Mình qua lấy lúc 5h chiều nhé.","Thứ 6",2,true));
        entry(s,"Phí thuê máy chiếu mini",380000,EntryType.INCOME,DEMO);
        entry(s,"Cho thuê thiết bị tháng 12",4440000,EntryType.INCOME,DEMO.minusDays(1));
        entry(s,"Phí thuê máy ảnh Canon",-660000,EntryType.EXPENSE,DEMO.minusDays(2));
        entry(s,"Phí thuê đồ dùng",-1000000,EntryType.EXPENSE,DEMO.minusDays(3));
        entry(s,"Tiền cọc đang giữ",1200000,EntryType.HELD,DEMO.minusDays(4));
        entry(s,"Tiền cọc đã hoàn",-1000000,EntryType.REFUND,DEMO.minusDays(5));
        entry(s,"Khấu trừ tiền cọc",-150000,EntryType.DEDUCTION,DEMO.minusDays(6));
        long[] income={3600000,2700000,3800000,2100000,3100000,2400000};
        long[] expense={1300000,1400000,1900000,800000,1700000,1200000};
        for(int i=0;i<6;i++){
            LocalDate date=DEMO.minusMonths(i+1);
            entry(s,"Cho thuê thiết bị tháng "+date.getMonthValue(),income[i],EntryType.INCOME,date);
            entry(s,"Thuê đồ tháng "+date.getMonthValue(),-expense[i],EntryType.EXPENSE,date);
        }
        s.preferences.put("Tin nhắn mới",true);
        s.preferences.put("Yêu cầu & trạng thái giao dịch",true);
        s.preferences.put("Xác nhận thanh toán",true);
        s.preferences.put("Nhắc bàn giao và trả đồ",false);
        s.preferences.put("Hiển thị số điện thoại",false);
        s.unavailable.put(1,new HashSet<>(Arrays.asList(DEMO.minusDays(3),DEMO.plusDays(5),DEMO.plusDays(6),DEMO.plusDays(13))));
        s.unavailable.put(2,new HashSet<>(Arrays.asList(DEMO.plusDays(8),DEMO.plusDays(9))));
        s.assistant.add(Message.text("Tôi cần thuê laptop để học 1 tuần, ngân sách dưới 500.000đ.",true,"10:00"));
        s.assistant.add(Message.text("Bạn có thể tham khảo Acer Aspire 5 và Dell Inspiron 15. Bạn muốn nhận đồ ở khu vực nào?",false,"10:00"));
        return s;
    }
    @Override public void authenticate(String email,String password,boolean registration) {
        // Demo authentication accepts validated inputs and never stores the password.
    }
    @Override public String answer(String question) {
        String q=search(question);
        if(q.contains("coc")) return "Tiền cọc tách riêng khỏi phí thuê, được hoàn lại khi hai bên xác nhận đã trả đồ. Có thể khấu trừ nếu hai bên thống nhất.";
        if(q.contains("muon")) return "Trong Khám phá, chọn Miễn phí để tìm đồ cho mượn. Bạn vẫn có thể cần đặt cọc theo thỏa thuận với chủ đồ.";
        if(q.contains("laptop")||q.contains("tuan")||q.contains("ngan sach"))
            return "Bạn có thể tham khảo Acer Aspire 5 (350.000đ/tuần) và Dell Inspiron 15 (420.000đ/tuần). Bạn muốn nhận đồ ở đâu?";
        return "Bạn cần loại đồ gì, ngân sách bao nhiêu và muốn nhận ở khu vực nào?";
    }
    private void add(State s,int id,String name,Art art,long rate,long deposit,String area,int quantity,String owner,
                     String category,String description,boolean own,Status status,int age,int reviews,int rentals,String note) {
        s.products.add(new Product(id,name,art,rate,deposit,area,quantity,owner,category,id==2?"Đã dùng":"Như mới",
                description,own,status,DEMO.minusDays(age),reviews,rentals,Collections.emptyList(),note));
    }
    private void chat(State s,int id,String message,String time,int unread) {
        String owner=s.product(id).owner;
        s.chats.put(id,new Conversation(id,owner,initials(owner),unread,Message.text(message,false,time)));
    }
    private void tx(State s,int id,int product,long rate,long deposit,int days,Role role,int count) {
        Transaction t=new Transaction(id,product,role==Role.OWNER?"Bùi Thảo":s.product(product).owner,role,DEMO,days,rate,deposit);
        for(int i=0;i<count;i++){Step step=Step.values()[i];t.confirm(step,step.role);}
        s.transactions.add(t);
    }
    private void entry(State s,String title,long amount,EntryType type,LocalDate date){s.entries.add(new Entry(title,amount,type,date));}
}
