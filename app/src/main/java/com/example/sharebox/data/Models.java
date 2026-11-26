package com.example.sharebox.data;

import java.time.LocalDate;
import java.text.NumberFormat;
import java.text.Normalizer;
import java.util.*;

public final class Models {
    private Models() {}
    public static final LocalDate DEMO = LocalDate.of(2026, 12, 8);
    public static final String QR_NOTE = "QR do người dùng cung cấp. ShareBox không xác minh thanh toán. Vui lòng kiểm tra thông tin trước khi chuyển tiền.";
    public static final String EXTERNAL_PAYMENT = "Thanh toán ngoài ứng dụng. ShareBox không có ví và không tự xác minh chuyển khoản. Người nhận tự kiểm tra rồi xác nhận.";

    public enum Art { CAMERA, DRILL, LAPTOP, CHAIR, SPEAKER, BOOK, BIKE }
    public enum Usage { RENT, BORROW }
    public enum Status {
        ACTIVE("Đang hiển thị"), HIDDEN("Đã ẩn"), REMOVED("Đã gỡ");
        public final String label;
        Status(String label) { this.label = label; }
    }
    public enum Stage {
        PENDING("Chờ xác nhận"), PAYMENT("Chờ thanh toán"), HANDOVER("Chờ bàn giao"),
        ACTIVE("Đang thuê"), RETURN("Chờ xác nhận trả"), COMPLETE("Hoàn tất"), CANCELLED("Đã hủy");
        public final String label;
        Stage(String label) { this.label = label; }
    }
    public enum Role { RENTER, OWNER }
    public enum Step {
        ACCEPT(Role.OWNER,"Duyệt yêu cầu"), PAID(Role.RENTER,"Tôi đã thanh toán"),
        RECEIVED_MONEY(Role.OWNER,"Xác nhận đã nhận tiền"), HANDED(Role.OWNER,"Xác nhận đã giao đồ"),
        RECEIVED_ITEM(Role.RENTER,"Tôi đã nhận đồ"), RETURNED(Role.RENTER,"Xác nhận đã trả đồ"),
        RECEIVED_BACK(Role.OWNER,"Xác nhận đã nhận lại");
        public final Role role;
        public final String label;
        Step(Role role, String label) { this.role = role; this.label = label; }
    }
    public enum NoticeType {
        CHAT("Tin nhắn"), REQUEST("Yêu cầu"), PAYMENT("Thanh toán"), REMINDER("Nhắc nhở"), STATUS("Trạng thái");
        public final String label;
        NoticeType(String label) { this.label = label; }
    }
    public enum EntryType {
        INCOME("Thu"), EXPENSE("Chi"), HELD("Đang giữ"), REFUND("Đã hoàn"), DEDUCTION("Khấu trừ");
        public final String label;
        EntryType(String label) { this.label = label; }
        public boolean deposit() { return this != INCOME && this != EXPENSE; }
    }

    public static final class Product {
        public final int id;
        public final String name, owner, category, location, condition, description, note;
        public final Art art;
        public final long rate, deposit;
        public final int quantity, goodReviews, rentals;
        public final boolean own;
        public final Status status;
        public final LocalDate posted;
        public final List<String> images;
        public Product(int id, String name, Art art, long rate, long deposit, String location, int quantity,
                       String owner, String category, String condition, String description, boolean own,
                       Status status, LocalDate posted, int goodReviews, int rentals, List<String> images, String note) {
            this.id=id; this.name=name; this.art=art; this.rate=rate; this.deposit=deposit;
            this.location=location; this.quantity=quantity; this.owner=owner; this.category=category;
            this.condition=condition; this.description=description; this.own=own; this.status=status;
            this.posted=posted; this.goodReviews=goodReviews; this.rentals=rentals;
            this.images=Collections.unmodifiableList(new ArrayList<>(images)); this.note=note;
        }
        public Usage usage() { return rate == 0 ? Usage.BORROW : Usage.RENT; }
        public boolean hot() { return goodReviews >= 20 && rentals >= 30; }
        public boolean recent() { return !posted.isBefore(DEMO.minusDays(3)); }
        public String price() { return rate == 0 ? "Miễn phí" : money(rate) + "/ngày"; }
        public Product status(Status next) {
            return new Product(id,name,art,rate,deposit,location,quantity,owner,category,condition,description,
                    own,next,posted,goodReviews,rentals,images,note);
        }
    }

    public static final class Profile {
        public final String name,email,phone,area,bio,photo;
        public Profile(String name,String email,String phone,String area,String bio,String photo) {
            this.name=name; this.email=email; this.phone=phone; this.area=area; this.bio=bio; this.photo=photo;
        }
    }
    public static final class Message {
        public final String text, time, uri;
        public final boolean mine, qr;
        public final Art art;
        public Message(String text,boolean mine,String time,String uri,Art art,boolean qr) {
            this.text=text;this.mine=mine;this.time=time;this.uri=uri;this.art=art;this.qr=qr;
        }
        public static Message text(String text,boolean mine,String time) { return new Message(text,mine,time,null,null,false); }
    }
    public static final class Conversation {
        public final int productId;
        public final String name, initials;
        public final List<Message> messages = new ArrayList<>();
        public int unread;
        public boolean archived, muted;
        public Conversation(int productId,String name,String initials,int unread,Message... seed) {
            this.productId=productId;this.name=name;this.initials=initials;this.unread=unread;
            messages.addAll(Arrays.asList(seed));
        }
        public Message latest() { return messages.isEmpty() ? Message.text("Bắt đầu trò chuyện",false,"") : messages.get(messages.size()-1); }
    }

    public static final class Transaction {
        public final int id, productId;
        public final String contact;
        public final Role myRole;
        public final LocalDate start;
        public final int days;
        public final long rate, deposit;
        private final EnumSet<Step> checks = EnumSet.noneOf(Step.class);
        private boolean disputed, cancelled;
        public long deduction;
        public boolean transfer = true;
        public String proof, qr, disputeReason = "", review = "";

        public Transaction(int id,int productId,String contact,Role myRole,LocalDate start,int days,long rate,long deposit) {
            if (days < 1 || days > 14 || rate < 0 || deposit < 0) throw new IllegalArgumentException("Khoảng thuê không hợp lệ");
            this.id=id;this.productId=productId;this.contact=contact;this.myRole=myRole;
            this.start=start;this.days=days;this.rate=rate;this.deposit=deposit;
        }
        public long fee() { return rate * days; }
        public long total() { return fee()+deposit; }
        public LocalDate end() { return start.plusDays(days-1); }
        public boolean done(Step step) { return checks.contains(step); }
        public boolean disputed() { return disputed; }
        public boolean terminal() { return cancelled || stage()==Stage.COMPLETE; }
        public String label() {
            if (disputed) return "Tranh chấp";
            if (stage()==Stage.ACTIVE && rate==0) return myRole==Role.OWNER ? "Đang cho mượn" : "Đang mượn";
            return stage().label;
        }
        public Stage stage() {
            if (cancelled) return Stage.CANCELLED;
            if (!done(Step.ACCEPT)) return Stage.PENDING;
            if (!done(Step.RECEIVED_MONEY)) return Stage.PAYMENT;
            if (!done(Step.HANDED) || !done(Step.RECEIVED_ITEM)) return Stage.HANDOVER;
            if (!done(Step.RETURNED) && !done(Step.RECEIVED_BACK)) return Stage.ACTIVE;
            return done(Step.RETURNED) && done(Step.RECEIVED_BACK) ? Stage.COMPLETE : Stage.RETURN;
        }
        public List<Step> actions() {
            if (terminal() || disputed) return Collections.emptyList();
            switch(stage()) {
                case PENDING: return Collections.singletonList(Step.ACCEPT);
                case PAYMENT: return Collections.singletonList(done(Step.PAID)?Step.RECEIVED_MONEY:Step.PAID);
                case HANDOVER: return missing(Step.HANDED,Step.RECEIVED_ITEM);
                case ACTIVE:
                case RETURN: return missing(Step.RETURNED,Step.RECEIVED_BACK);
                default: return Collections.emptyList();
            }
        }
        private List<Step> missing(Step a, Step b) {
            List<Step> result=new ArrayList<>();
            if (!done(a)) result.add(a);
            if (!done(b)) result.add(b);
            return result;
        }
        public boolean confirm(Step step, Role actor) {
            return step.role==actor && actions().contains(step) && checks.add(step);
        }
        public void dispute(String reason) {
            if (!terminal() && !reason.trim().isEmpty()) { disputed=true; disputeReason=reason.trim(); }
        }
        public void clearDispute() { disputed=false; disputeReason=""; }
        public void cancel() { if (!terminal() && !disputed) cancelled=true; }
        public long refund() { return stage()==Stage.COMPLETE && !disputed ? deposit - deduction : 0L; }
    }

    public static final class Notice {
        public final int id, target;
        public final NoticeType type;
        public final String title,body,time;
        public boolean read;
        public Notice(int id,NoticeType type,String title,String body,String time,int target,boolean read) {
            this.id=id;this.type=type;this.title=title;this.body=body;this.time=time;this.target=target;this.read=read;
        }
    }
    public static final class Entry {
        public final String title;
        public final long amount;
        public final EntryType type;
        public final LocalDate date;
        public Entry(String title,long amount,EntryType type,LocalDate date) {
            this.title=title;this.amount=amount;this.type=type;this.date=date;
        }
    }
    public static final class State {
        public final List<Product> products=new ArrayList<>();
        public final Map<Integer,Conversation> chats=new LinkedHashMap<>();
        public final Set<String> blocked=new HashSet<>();
        public final Set<Integer> favorites=new LinkedHashSet<>();
        public final List<Transaction> transactions=new ArrayList<>();
        public final List<Notice> notices=new ArrayList<>();
        public final List<Entry> entries=new ArrayList<>();
        public final List<Message> assistant=new ArrayList<>();
        public final Map<String,Boolean> preferences=new LinkedHashMap<>();
        public final Map<Integer,Set<LocalDate>> unavailable=new HashMap<>();
        public final List<String> reports=new ArrayList<>();
        public Profile profile=new Profile("Nguyễn Minh Anh","minhanh@gmail.com","0901234567","Thủ Đức, TP.HCM",
                "Sinh viên UIT, thích chia sẻ đồ dùng học tập và thiết bị công nghệ.",null);
        public boolean signedIn, dark, assistantHidden, assistantBusy;
        public Product product(int id) { for (Product p:products) if(p.id==id)return p; return null; }
        public Transaction transaction(int id) { for(Transaction t:transactions) if(t.id==id)return t; return null; }
        public boolean available(int id,LocalDate start,int days) {
            if(days<1 || days>14) return false;
            Set<LocalDate> busy=unavailable.getOrDefault(id,Collections.emptySet());
            Product item=product(id);
            if(item==null || item.status==Status.REMOVED)return false;
            for(int i=0;i<days;i++){
                LocalDate day=start.plusDays(i);
                if(busy.contains(day))return false;
                long reserved=transactions.stream().filter(t->t.productId==id&&!t.terminal()&&t.done(Step.ACCEPT)
                        &&!day.isBefore(t.start)&&!day.isAfter(t.end())).count();
                if(reserved>=item.quantity)return false;
            }
            return true;
        }
    }
    public static String money(long amount) { return NumberFormat.getIntegerInstance(Locale.forLanguageTag("vi-VN")).format(amount)+"đ"; }
    public static String search(String text) {
        return Normalizer.normalize(text,Normalizer.Form.NFD).replaceAll("\\p{M}+","").toLowerCase(Locale.ROOT).replace('đ','d').trim();
    }
    public static String initials(String text) {
        String[] parts=text.trim().split("\\s+");
        StringBuilder b=new StringBuilder();
        for(int i=Math.max(0,parts.length-2);i<parts.length;i++) if(!parts[i].isEmpty()) b.appendCodePoint(parts[i].codePointAt(0));
        return b.toString().toUpperCase(Locale.ROOT);
    }
}
