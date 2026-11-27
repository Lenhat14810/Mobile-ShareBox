package com.example.sharebox.ui;

import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import com.example.sharebox.R;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import static com.example.sharebox.data.Models.*;

public final class TransactionPages {
    private TransactionPages() {}

    private abstract static class TransactionPage extends Page {
        TransactionPage(int layout){super(layout);}
        int tone(Transaction t){
            if(t.disputed()||t.stage()==Stage.CANCELLED)return R.color.sb_error;
            if(t.stage()==Stage.COMPLETE)return R.color.sb_success;
            return t.stage()==Stage.PENDING||t.stage()==Stage.PAYMENT||t.stage()==Stage.RETURN?R.color.sb_warning:R.color.sb_accent;
        }
        void totals(LinearLayout card,long fee,long deposit){
            details(card,"Phí thuê / mượn",money(fee));
            details(card,"Tiền cọc (hoàn lại khi trả đồ)",deposit>0?money(deposit):"Không yêu cầu");
            details(card,"Tổng thanh toán",money(fee+deposit));
        }
        void status(LinearLayout group,String label,boolean checked){
            LinearLayout row=row();
            TextView icon=text(checked?"✓":"◷",14,checked?R.color.sb_success:R.color.sb_muted,true);
            icon.setMinWidth(dp(22));row.addView(icon);
            weight(row,body(label));
            TextView confirmation=text(checked?"Đã xác nhận":"Chờ xác nhận",12,
                    checked?R.color.sb_success:R.color.sb_warning,true);
            confirmation.setPaddingRelative(dp(8),0,0,0);
            row.addView(confirmation);
            add(group,row,7);
        }
        String actionLabel(Transaction t,Step step){
            return (step.role==t.myRole?"":step.role==Role.OWNER?"Mô phỏng chủ đồ: ":"Mô phỏng người thuê: ")+step.label;
        }
        void confirmStep(Transaction t,Step step){
            String message=step.role!=t.myRole?"Mô phỏng xác nhận của phía còn lại trong giao dịch mẫu.":"Ghi nhận xác nhận của bạn cho bước này.";
            confirm(actionLabel(t,step),message,"Xác nhận",()->{
                if(vm.confirm(t,step,step.role)){shortNote("Đã lưu xác nhận");refresh();}
                else shortNote("Bước này chưa thể xác nhận. Kiểm tra trạng thái và ngày thuê.");
            });
        }
    }

    public static final class Request extends TransactionPage {
        public Request(){super(R.layout.screen_tx_request);}
        @Override protected void render(){
            int original=itemId();
            Product p=state.product(memory.getInt("product",original));
            if(p==null){missing("Không tìm thấy sản phẩm");return;}
            header();
            LocalDate start=LocalDate.ofEpochDay(args().getLong("start",DEMO.toEpochDay()));
            int days=memory.getInt("days",args().getInt("days",3));
            boolean useDeposit=p.rate>0||memory.getBoolean("deposit",true);
            long rate=p.id==1?220000:p.rate, deposit=useDeposit?p.deposit:0;
            LinearLayout content=frame.content;
            note(content,"Yêu cầu được tạo từ cuộc trò chuyện hiện có với "+p.owner+".",R.color.sb_accent);
            if((original==1||original==2)&&state.chats.containsKey(1)&&state.chats.containsKey(2)){
                chips(content,new String[]{"Máy ảnh Canon (thuê)","Máy khoan Bosch (miễn phí)"},p.id-1,i->{
                    memory.putInt("product",i+1);memory.remove("sent");refresh();
                });
            }
            LinearLayout card=panel();
            add(card,productSummary(p,"Chủ đồ: "+p.owner),0);
            add(card,text(rate>0?"Giá đã thống nhất: "+money(rate)+"/ngày":"Miễn phí",13,rate>0?R.color.sb_accent:R.color.sb_success,true),8);
            add(content,card,12);
            LinearLayout duration=panel();
            add(duration,title("Thời gian thuê / mượn"),0);
            LinearLayout dates=row();
            LinearLayout from=col(),to=col();
            add(from,meta("Ngày nhận"),0);add(from,body("▣ "+date(start)),8);
            add(to,meta("Ngày trả"),0);add(to,body("▣ "+date(start.plusDays(days-1))),8);
            weight(dates,from);weight(dates,to);add(duration,dates,13);
            LinearLayout stepper=row();
            weight(stepper,body("Số ngày"));
            TextView decrease=button("−",false,()->setDays(days-1));decrease.setContentDescription("Giảm số ngày");decrease.setEnabled(days>1);
            TextView increase=button("+",false,()->setDays(days+1));increase.setContentDescription("Tăng số ngày");increase.setEnabled(days<14);
            stepper.addView(decrease);
            TextView number=title(""+days);number.setGravity(Gravity.CENTER);number.setMinWidth(dp(36));stepper.addView(number);stepper.addView(increase);
            add(duration,stepper,10);
            boolean valid=state.available(p.id,start,days);
            if(!valid)note(duration,"Khoảng ngày có ngày đã có người thuê. Hãy giảm số ngày hoặc chọn lại trên lịch.",R.color.sb_error);
            add(content,duration,12);
            LinearLayout terms=panel();
            add(terms,title("Tiền đặt cọc"),0);
            if(p.rate==0){
                Switch toggle=new Switch(requireContext());toggle.setText("Yêu cầu đặt cọc");toggle.setMinHeight(dp(48));
                toggle.setChecked(useDeposit);toggle.setTextColor(color(R.color.sb_ink));
                toggle.setOnCheckedChangeListener((v,on)->{memory.putBoolean("deposit",on);memory.remove("sent");refresh();});
                add(terms,toggle,5);
            }
            add(terms,meta(deposit>0?money(deposit)+(p.rate>0?" · Bắt buộc":" · Tùy chọn"):"Không yêu cầu"),6);
            note(terms,"Điều khoản\nSử dụng đúng mục đích, giữ gìn cẩn thận, trả đúng hạn. Hai bên cùng xác nhận bàn giao và hoàn trả.",R.color.sb_ink);
            add(content,terms,12);
            LinearLayout summary=panel();
            add(summary,title("Tóm tắt giao dịch"),0);
            totals(summary,rate*days,deposit);
            details(summary,"Thời gian",date(start)+" – "+date(start.plusDays(days-1)));
            add(summary,meta("Thanh toán ngoài ứng dụng."),8);add(content,summary,12);
            if(memory.containsKey("sent")){
                note(content,"Đã gửi yêu cầu tới "+p.owner+". Chờ chủ đồ xác nhận.",R.color.sb_success);
                footer("Xem giao dịch của tôi",true,()->open(R.id.transactions));
            }else {
                boolean allow=valid&&!vm.blocked(p);
                if(!allow&&vm.blocked(p))note(content,"Bạn không thể gửi yêu cầu cho người dùng này.",R.color.sb_error);
                if(allow)footer("Gửi yêu cầu",true,()->{
                    Transaction created=vm.request(p,start,days,useDeposit);
                    if(created!=null){memory.putInt("sent",created.id);shortNote("Đã gửi yêu cầu");refresh();}
                });
            }
        }
        private void setDays(int days){
            if(days>=1&&days<=14){memory.putInt("days",days);memory.remove("sent");refresh();}
        }
    }

    public static final class ListPage extends TransactionPage {
        public ListPage(){super(R.layout.screen_transactions);}
        @Override protected void render(){
            header();frame.header.addView(icon(R.drawable.sb_ic_finance,"Tài chính",()->open(R.id.finance)));
            int filter=memory.getInt("filter");
            String[] labels={"Tất cả","Chờ xác nhận","Đang diễn ra","Hoàn tất","Đã hủy"};
            for(int i=0;i<labels.length;i++){
                final int tab=i;
                labels[i]+=" "+state.transactions.stream().filter(t->matches(t,tab)).count();
            }
            frame.above.setPadding(dp(16),0,dp(16),dp(5));
            flatFilters(frame.above,labels,i->i==memory.getInt("filter"),i->{memory.putInt("filter",i);refresh();});
            List<Transaction> visible=state.transactions.stream().filter(t->matches(t,filter)).collect(Collectors.toList());
            if(visible.isEmpty())empty(frame.content,"◇","Chưa có giao dịch","Giao dịch sẽ xuất hiện tại đây.","",null);
            else pageList(visible,1,this::card);
        }
        boolean matches(Transaction t,int tab){
            if(tab==0)return true;
            switch(t.stage()){
                case PENDING:return tab==1;case COMPLETE:return tab==3;case CANCELLED:return tab==4;default:return tab==2;
            }
        }
        View card(Transaction t){
            LinearLayout card=panel();LinearLayout row=row();
            weight(row,text(t.myRole==Role.RENTER?"Bạn thuê":"Bạn cho thuê",12,R.color.sb_muted,true));
            TextView status=text(t.label(),13,tone(t),true);
            status.setGravity(Gravity.END);
            status.setPaddingRelative(dp(8),0,0,0);
            weight(row,status);
            add(card,row,0);
            Product p=state.product(t.productId);
            if(p!=null)add(card,productSummary(p,(t.myRole==Role.RENTER?"Chủ đồ: ":"Người yêu cầu: ")+t.contact),8);
            LinearLayout end=row();
            LinearLayout money=col();add(money,meta(t.myRole==Role.OWNER?"Phí thuê":t.rate==0?"Tiền cọc":"Tổng cần trả (gồm cọc)"),0);
            add(money,title(money(t.myRole==Role.OWNER?t.fee():t.total())),2);weight(end,money);
            String label=t.stage()==Stage.COMPLETE?"Đánh giá":t.actions().stream().filter(s->s.role==t.myRole).findFirst()
                    .map(s->s.label).orElse("Xem chi tiết");
            end.addView(button(label,!label.equals("Xem chi tiết"),()->open(R.id.tx_detail,t.id)));
            add(card,end,12);click(card,R.drawable.sb_card,()->open(R.id.tx_detail,t.id));
            return card;
        }
    }

    public static final class Detail extends TransactionPage {
        public Detail(){super(R.layout.screen_tx_detail);}
        @Override protected void render(){
            Transaction t=transaction();
            if(t==null){missing("Không tìm thấy giao dịch");return;}
            header();
            TextView status=text(t.label(),12,tone(t),true);
            status.setMaxWidth(dp(116));
            status.setGravity(Gravity.END);
            frame.header.addView(status);
            Product p=state.product(t.productId);
            LinearLayout card=panel();
            if(p!=null)add(card,productSummary(p,t.contact+" · "+date(t.start)+" – "+date(t.end())),0);
            add(frame.content,card,4);
            if(t.disputed()){
                note(frame.content,"Giao dịch đang tranh chấp. Hai bên chưa thống nhất xác nhận. Giao dịch sẽ không tự động hoàn tất.\n"+t.disputeReason,R.color.sb_error);
                add(frame.content,button("Liên hệ hỗ trợ",false,()->open(R.id.settings)),6);
            }
            LinearLayout costs=panel();totals(costs,t.fee(),t.deposit);
            add(costs,button("Xem thanh toán & đặt cọc  ›",false,()->open(R.id.payment,t.id)),10);
            add(frame.content,costs,12);
            LinearLayout flow=panel();add(flow,title("Tiến trình giao dịch"),0);
            line(flow,"1","Gửi yêu cầu",true);
            line(flow,"2","Chủ đồ chấp nhận",t.done(Step.ACCEPT));
            line(flow,"3","Thanh toán & đặt cọc",t.done(Step.RECEIVED_MONEY));
            status(flow,"Người thuê đã thanh toán",t.done(Step.PAID));
            status(flow,"Chủ đồ đã nhận tiền",t.done(Step.RECEIVED_MONEY));
            line(flow,"4","Bàn giao đồ",t.done(Step.HANDED)&&t.done(Step.RECEIVED_ITEM));
            status(flow,"Chủ đồ đã giao đồ",t.done(Step.HANDED));
            status(flow,"Người thuê đã nhận đồ",t.done(Step.RECEIVED_ITEM));
            line(flow,"5",t.rate==0?"Đang mượn":"Đang thuê",t.done(Step.HANDED)&&t.done(Step.RECEIVED_ITEM));
            line(flow,"6","Trả đồ",t.done(Step.RETURNED)&&t.done(Step.RECEIVED_BACK));
            status(flow,"Người thuê đã trả đồ",t.done(Step.RETURNED));
            status(flow,"Chủ đồ đã nhận lại",t.done(Step.RECEIVED_BACK));
            line(flow,"7","Hoàn tất",t.stage()==Stage.COMPLETE);
            add(frame.content,flow,12);
            if(t.stage()==Stage.COMPLETE){
                note(frame.content,"Cả hai bên đã xác nhận. Giao dịch hoàn tất.",R.color.sb_success);
                if(t.deposit>0){details(frame.content,"Đã hoàn cọc",money(t.refund()));details(frame.content,"Khấu trừ",money(t.deduction));}
                add(frame.content,button(t.review.isEmpty()?"Đánh giá":"Sửa đánh giá",true,()->review(t)),8);
                if(!t.review.isEmpty())add(frame.content,meta(t.review),6);
                add(frame.content,button("Chạy lại luồng demo",false,()->{
                    if(p!=null){
                        Transaction next=vm.request(p,t.start,t.days,t.deposit>0);
                        if(next!=null)open(R.id.tx_detail,next.id);else shortNote("Ngày đã được đặt hoặc người dùng đã bị chặn.");
                    }
                }),6);
                return;
            }
            if(t.stage()==Stage.CANCELLED){note(frame.content,"Giao dịch đã hủy.",R.color.sb_error);return;}
            if(t.disputed()){
                footer("Hủy báo tranh chấp",false,()->confirm("Hủy báo tranh chấp?","Tiếp tục từ bước xác nhận đang dở.","Hủy báo",()->{t.clearDispute();vm.changed();refresh();}));
                return;
            }
            if(t.stage()==Stage.PAYMENT){
                footer("Xem thanh toán & đặt cọc",true,()->open(R.id.payment,t.id));
            }else{
                List<Step> actions=new ArrayList<>(t.actions());
                actions.sort(Comparator.comparingInt(step->step.role==t.myRole?0:1));
                for(Step step:actions){
                    if(step==actions.get(0))footer(actionLabel(t,step),true,()->step(t,step));
                    else add(frame.content,button(actionLabel(t,step),false,()->step(t,step)),9);
                }
            }
            add(frame.content,button("Hủy giao dịch",false,()->confirm("Hủy giao dịch?","Trạng thái sẽ đổi thành Đã hủy.","Hủy giao dịch",()->{t.cancel();vm.changed();refresh();})),10);
            footer("⚐  Báo tranh chấp",false,()->dispute(t));
        }
        private void line(LinearLayout card,String number,String step,boolean done){
            add(card,text((done?"✓":number)+"   "+step,14,done?R.color.sb_success:R.color.sb_ink,true),12);
        }
        private void step(Transaction t,Step step){
            if(step!=Step.RECEIVED_BACK||t.deposit==0){confirmStep(t,step);return;}
            LinearLayout view=col();view.setPadding(dp(16),0,dp(16),dp(8));
            add(view,meta("Chỉ ghi khoản đã được hai bên thống nhất."),4);
            EditText amount=field(view,"Khấu trừ cọc (tùy chọn)","0",t.deduction==0?"":""+t.deduction,InputType.TYPE_CLASS_NUMBER,1);
            AlertDialog d=new AlertDialog.Builder(requireContext()).setTitle(actionLabel(t,step)).setView(view)
                    .setNegativeButton("Hủy",null).setPositiveButton("Xác nhận",null).create();
            d.setOnShowListener(x->d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
                long n=number(amount);
                if(n>t.deposit||n<0){amount.setError("Khấu trừ tối đa "+money(t.deposit));return;}
                t.deduction=n;
                if(vm.confirm(t,step,step.role)){d.dismiss();refresh();}
            }));
            d.show();
        }
        private void dispute(Transaction t){
            LinearLayout view=col();view.setPadding(dp(16),0,dp(16),dp(10));
            EditText reason=field(view,"Lý do","Mô tả tranh chấp","",InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_MULTI_LINE,3);
            AlertDialog dialog=new AlertDialog.Builder(requireContext()).setTitle("Báo tranh chấp").setView(view)
                    .setNegativeButton("Hủy",null).setPositiveButton("Gửi báo cáo",null).create();
            dialog.setOnShowListener(x->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
                if(value(reason).isEmpty()){reason.setError("Vui lòng nhập lý do");return;}
                t.dispute(value(reason));vm.changed();dialog.dismiss();refresh();
            }));
            dialog.show();
        }
        private void review(Transaction t){
            LinearLayout view=col();view.setPadding(dp(16),0,dp(16),dp(10));
            RatingBar stars=new RatingBar(requireContext());stars.setNumStars(5);stars.setStepSize(1);stars.setRating(5);
            view.addView(stars,new LinearLayout.LayoutParams(-2,-2));
            EditText note=field(view,"Nhận xét","Chia sẻ trải nghiệm","",InputType.TYPE_CLASS_TEXT,2);
            new AlertDialog.Builder(requireContext()).setTitle("Đánh giá").setView(view).setNegativeButton("Hủy",null)
                    .setPositiveButton("Lưu",(d,n)->{t.review=(int)stars.getRating()+" ★  "+value(note);vm.changed();refresh();}).show();
        }
    }

    public static final class Payment extends TransactionPage {
        private boolean qrSelection;
        private final ActivityResultLauncher<String> attach=registerForActivityResult(new ActivityResultContracts.GetContent(),this::received);
        public Payment(){super(R.layout.screen_payment);}
        @Override protected void render(){
            Transaction t=transaction();
            if(t==null){missing("Không tìm thấy giao dịch");return;}
            Product p=state.product(t.productId);
            header();
            String status=t.done(Step.RECEIVED_MONEY)?"Đã xác nhận":t.done(Step.PAID)?"Chờ người nhận xác nhận":"Chưa thanh toán";
            banner(frame.content,"Số tiền cần thanh toán",money(t.total()),status+"  ·  "+(p==null?"":p.name+"  ·  ")+t.days+" ngày");
            LinearLayout costs=panel();totals(costs,t.fee(),t.deposit);add(frame.content,costs,12);
            LinearLayout method=panel();add(method,title("Phương thức thanh toán"),0);
            boolean editable=!t.disputed()&&!t.done(Step.PAID)&&!t.terminal();
            chips(method,new String[]{"Tiền mặt","Chuyển khoản"},t.transfer?1:0,i->{if(editable){t.transfer=i==1;vm.changed();refresh();}});
            if(t.transfer){
                details(method,"Ngân hàng","Vietcombank");
                details(method,"Số tài khoản","•••• •••• 6789");
                details(method,"Chủ tài khoản",t.myRole==Role.OWNER?state.profile.name:t.contact);
                add(method,button("Xem mã QR",false,()->preview(t,t.qr,true)),8);
                if(editable)add(method,button("Thêm mã QR",false,()->pick(true)),4);
            }else note(method,"Thanh toán khi gặp trực tiếp lúc bàn giao.",R.color.sb_muted);
            add(frame.content,method,12);
            LinearLayout checks=panel();add(checks,title("Trạng thái thanh toán"),0);
            status(checks,"Người trả đánh dấu đã thanh toán",t.done(Step.PAID));
            status(checks,"Người nhận xác nhận đã nhận tiền",t.done(Step.RECEIVED_MONEY));
            if(t.transfer&&t.done(Step.PAID)&&t.proof!=null)add(checks,button("Xem ảnh chứng từ",false,()->preview(t,t.proof,false)),8);
            add(frame.content,checks,12);
            note(frame.content,EXTERNAL_PAYMENT,R.color.sb_warning);
            if(t.stage()==Stage.PAYMENT&&!t.disputed()){
                if(t.transfer&&!t.done(Step.RECEIVED_MONEY))
                    footer(t.proof==null?"Tải ảnh chứng từ (tùy chọn)":"Thay ảnh chứng từ",false,()->pick(false));
                Step next=t.done(Step.PAID)?Step.RECEIVED_MONEY:Step.PAID;
                footer(actionLabel(t,next),true,()->confirmStep(t,next));
                if(t.done(Step.PAID))note(frame.content,"Đang chờ chủ đồ kiểm tra tiền.",R.color.sb_warning);
            }
            if(t.done(Step.RECEIVED_MONEY)){
                note(frame.content,"Đã xác nhận nhận tiền. Tiếp theo là bàn giao đồ.",R.color.sb_success);
                footer("Về chi tiết giao dịch",true,()->{
                    if(args().getBoolean("notice"))open(R.id.tx_detail,t.id);else back();
                });
            }
            if(t.stage()==Stage.COMPLETE&&t.deposit>0){
                details(frame.content,"Đã hoàn cọc",money(t.refund()));
                details(frame.content,"Khấu trừ",money(t.deduction));
            }
        }
        private void pick(boolean qr){qrSelection=qr;attach.launch("image/*");}
        private void received(Uri uri){
            Transaction t=frame==null?null:transaction();
            if(uri==null||t==null||t.disputed()||t.terminal())return;
            if(qrSelection)t.qr=uri.toString();else t.proof=uri.toString();
            vm.changed();shortNote(qrSelection?"Đã thêm mã QR":"Đã chọn ảnh chứng từ");refresh();
        }
        private void preview(Transaction t,String uri,boolean qr){
            LinearLayout box=col();box.setPadding(dp(16),dp(4),dp(16),dp(14));
            ImageView photo=image(Art.CAMERA,uri,true);
            if(uri==null)photo.setImageResource(R.drawable.sb_ic_qr);
            box.addView(photo,new LinearLayout.LayoutParams(-1,dp(215)));
            if(qr)note(box,(uri==null?"Mã minh họa. ":"")+QR_NOTE,R.color.sb_warning);
            new AlertDialog.Builder(requireContext()).setTitle(qr?"Mã QR chuyển khoản":"Ảnh chứng từ").setView(box)
                    .setPositiveButton("Đóng",null).show();
        }
    }
}
