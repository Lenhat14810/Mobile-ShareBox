package com.example.sharebox.ui;

import android.content.res.ColorStateList;
import android.graphics.drawable.RippleDrawable;
import android.net.Uri;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.*;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import com.example.sharebox.R;
import com.example.sharebox.databinding.ComponentComposerBinding;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import static com.example.sharebox.data.Models.*;

public final class ChatPages {
    private ChatPages(){}

    private abstract static class ConversationPage extends Page {
        ConversationPage(int layout){super(layout);}
        protected void options(Conversation chat,Runnable after){
            Product p=state.product(chat.productId);
            if(p==null)return;
            boolean read=chat.unread==0, blocked=vm.blocked(p);
            choose("Tùy chọn cuộc trò chuyện",new String[]{
                    read?"Đánh dấu chưa đọc":"Đánh dấu đã đọc",chat.muted?"Bật thông báo":"Tắt thông báo",
                    blocked?"Bỏ chặn người dùng":"Chặn người dùng",
                    chat.archived?"Bỏ lưu trữ đoạn chat":"Lưu trữ đoạn chat","Xóa đoạn chat"
            },option->{
                switch(option){
                    case 0:chat.unread=read?1:0;vm.changed();shortNote(read?"Đã đánh dấu chưa đọc":"Đã đánh dấu đã đọc");after.run();break;
                    case 1:chat.muted=!chat.muted;vm.changed();after.run();break;
                    case 2:
                        if(blocked){state.blocked.remove(p.owner);vm.changed();after.run();}
                        else confirm("Chặn "+chat.name+"?","Họ sẽ không thể nhắn tin hay gửi yêu cầu thuê / mượn cho bạn.","Chặn",()->{
                            state.blocked.add(p.owner);vm.changed();after.run();
                        });
                        break;
                    case 3:chat.archived=!chat.archived;vm.changed();after.run();break;
                    case 4:
                        confirm("Xóa toàn bộ đoạn chat với "+chat.name+"?","Hành động này không thể hoàn tác.","Xóa",()->{
                            state.chats.remove(chat.productId);vm.changed();
                            if(this instanceof Chat)back();else after.run();
                        });
                        break;
                    default:break;
                }
            });
        }
        protected View bubble(Message message){
            LinearLayout outside=col();outside.setGravity(message.mine?Gravity.END:Gravity.START);
            LinearLayout inside=col();
            inside.setPadding(dp(12),dp(9),dp(12),dp(9));
            inside.setBackgroundResource(message.mine?R.drawable.sb_blue:R.drawable.sb_card);
            if(message.uri!=null||message.art!=null||message.qr){
                Art art=message.art==null?Art.CAMERA:message.art;
                ImageView image=image(art,message.uri,false);
                if(message.qr&&message.uri==null)image.setImageResource(R.drawable.sb_ic_qr);
                inside.addView(image,new LinearLayout.LayoutParams(-1,dp(message.qr?120:155)));
                add(inside,text(message.qr?QR_NOTE:"Ảnh máy thực tế",12,message.mine?R.color.sb_paper:R.color.sb_ink,false),4);
                if(message.mine)((TextView)inside.getChildAt(1)).setTextColor(WHITE);
                image.setContentDescription(message.qr?"Xem mã QR":"Xem ảnh máy thực tế");
                image.setOnClickListener(v->preview(message));
            }else{
                TextView text=body(message.text);
                if(message.mine)text.setTextColor(WHITE);
                add(inside,text,0);
            }
            int max=getResources().getConfiguration().screenWidthDp;
            outside.addView(inside,new LinearLayout.LayoutParams(dp(Math.min(310,Math.max(195,max-100))),-2));
            add(outside,meta(message.time+(message.mine?"  ✓✓":"")),4);
            return outside;
        }
        protected void preview(Message m){
            LinearLayout body=col();body.setPadding(dp(16),0,dp(16),dp(10));
            ImageView large=image(m.art==null?Art.CAMERA:m.art,m.uri,true);
            if(m.qr&&m.uri==null)large.setImageResource(R.drawable.sb_ic_qr);
            body.addView(large,new LinearLayout.LayoutParams(-1,dp(260)));
            if(m.qr)note(body,QR_NOTE,R.color.sb_warning);
            new AlertDialog.Builder(requireContext()).setTitle(m.qr?"Mã QR do người dùng cung cấp":"Ảnh máy thực tế")
                    .setView(body).setPositiveButton("Đóng",null).show();
        }
        protected void compose(LinearLayout bar,boolean assistant,Consumer<String> send,Runnable attach,Runnable qr){
            ComponentComposerBinding composer=ComponentComposerBinding.inflate(getLayoutInflater(),bar,false);
            composer.pickImage.setVisibility(attach==null?View.GONE:View.VISIBLE);
            composer.pickQr.setVisibility(qr==null?View.GONE:View.VISIBLE);
            if(attach!=null)composer.pickImage.setOnClickListener(v->attach.run());
            if(qr!=null)composer.pickQr.setOnClickListener(v->qr.run());
            EditText input=composer.messageInput;
            input.setHint(assistant?R.string.sb_assistant_message_hint:R.string.sb_message_hint);
            Runnable action=()->{String text=value(input);if(!text.isEmpty()){send.accept(text);input.setText("");}};
            input.setOnEditorActionListener((t,id,event)->{if(id==EditorInfo.IME_ACTION_SEND){action.run();return true;}return false;});
            composer.sendMessage.setOnClickListener(v->action.run());
            add(bar,composer.getRoot(),2);
        }
        protected void scrollLatest(int count){if(count>0)frame.items.post(()->{if(frame!=null)frame.items.scrollToPosition(count-1);});}
    }

    public static final class Inbox extends ConversationPage{
        private LinearLayout heading;
        public Inbox(){super(R.layout.screen_chats);}
        @Override protected void render(){
            frame.above.setPadding(dp(16),dp(16),dp(16),dp(5));
            LinearLayout ai=panel();ai.setBackgroundResource(R.drawable.sb_gradient);
            TextView name=title("✧   ShareBox AI");name.setTextColor(WHITE);add(ai,name,0);
            TextView assistant=meta("Trợ lý tự động – tìm đồ, so sánh giá thuê");assistant.setTextColor(0xffe5edff);add(ai,assistant,5);
            click(ai,R.drawable.sb_gradient,()->open(R.id.ai_chat));add(frame.above,ai,0);
            space(frame.above,12);
            searchBar(frame.above,"Tìm cuộc trò chuyện",memory.getString("query",""),s->{memory.putString("query",s);update();},null);
            heading=row();add(frame.above,heading,6);update();
        }
        private void update(){
            boolean archived=memory.getBoolean("archived");
            String q=search(memory.getString("query",""));
            List<Conversation> data=state.chats.values().stream().filter(c->{
                Product p=state.product(c.productId);
                return p!=null&&c.archived==archived&&search(c.name+" "+p.name).contains(q);
            }).collect(Collectors.toList());
            heading.removeAllViews();weight(heading,title(archived?"Đã lưu trữ":"Chat"));
            long count=state.chats.values().stream().filter(c->c.archived).count();
            if(archived||count>0)heading.addView(button(archived?"Quay lại Chat":"Đã lưu trữ ("+count+")",false,()->{
                memory.putBoolean("archived",!archived);update();
            }));
            frame.content.removeAllViews();
            if(data.isEmpty()){
                frame.items.setVisibility(View.GONE);frame.scroll.setVisibility(View.VISIBLE);
                empty(frame.content,"◇",archived?"Chưa có đoạn chat lưu trữ":"Chưa có cuộc trò chuyện",
                        archived?"Đoạn chat lưu trữ sẽ xuất hiện tại đây.":"Chat với chủ đồ từ trang chi tiết sản phẩm để bắt đầu.",
                        "Khám phá",archived?null:()->host.openTab(R.id.explore));
            }else pageList(data,1,this::conversationCard);
        }
        private View conversationCard(Conversation c){
            Product product=state.product(c.productId);Message latest=c.latest();
            LinearLayout panel=panel(), row=row();row.addView(avatar(c.initials));
            LinearLayout words=col();words.setPadding(dp(10),0,dp(4),0);
            LinearLayout title=row();
            boolean unread=c.unread>0;
            TextView name=text(c.name,14,unread?R.color.sb_accent:R.color.sb_ink,unread);
            name.setPaddingRelative(0,0,dp(4),0);
            weight(title,name);
            title.addView(meta(latest.time));add(words,title,0);
            add(words,meta("◇  "+product.name+(c.muted?"  ·  Đã tắt thông báo":"")),5);
            TextView line=text(vm.blocked(product)?"Bạn đã chặn người dùng này":(latest.mine?"Bạn: ":"")+latest.text,
                    12,vm.blocked(product)?R.color.sb_error:R.color.sb_muted,c.unread>0);
            line.setMaxLines(2);add(words,line,5);weight(row,words);
            if(unread){
                TextView badge=text(c.unread>99?"99+":Integer.toString(c.unread),11,R.color.sb_paper,true);
                badge.setTextColor(WHITE);
                badge.setGravity(Gravity.CENTER);
                badge.setMaxLines(1);
                badge.setAutoSizeTextTypeUniformWithConfiguration(8,11,1,android.util.TypedValue.COMPLEX_UNIT_SP);
                badge.setBackgroundResource(R.drawable.sb_unread_badge);
                badge.setContentDescription(getResources().getQuantityString(
                        R.plurals.sb_conversation_unread,c.unread,c.unread));
                row.addView(badge,new LinearLayout.LayoutParams(dp(28),dp(28)));
            }
            ImageButton menu=icon(R.drawable.sb_ic_more,getString(R.string.sb_conversation_options,c.name),
                    false,()->options(c,this::update));
            menu.setBackground(new RippleDrawable(ColorStateList.valueOf(color(R.color.sb_stroke)),
                    shape(color(unread?R.color.sb_paper:R.color.sb_tint),24),null));
            LinearLayout.LayoutParams menuSize=new LinearLayout.LayoutParams(dp(48),dp(48));
            menuSize.setMarginStart(dp(10));
            row.addView(menu,menuSize);
            add(panel,row,0);
            click(panel,unread?R.drawable.sb_tint:R.drawable.sb_card,()->openChat(product));
            return panel;
        }
    }

    public static final class Chat extends ConversationPage {
        private boolean qrSelection;
        private final ActivityResultLauncher<String> imagePicker=registerForActivityResult(new ActivityResultContracts.GetContent(),this::attach);
        public Chat(){super(R.layout.screen_chat);}
        @Override protected void render(){
            Product p=product();
            if(p==null){missing("Không tìm thấy cuộc trò chuyện");return;}
            Conversation c=vm.chat(p);
            header(c.name);frame.header.addView(button("⋯",false,()->options(c,this::refresh)));
            LinearLayout info=panel();
            add(info,productSummary(p,p.price()+"  ·  "+(vm.blocked(p)?"Đã chặn":"Đang hoạt động")),0);
            click(info,R.drawable.sb_card,()->open(R.id.product,p.id));add(frame.above,info,2);
            renderMessages(c,false);
            if(!vm.blocked(p)){
                LinearLayout hint=col();hint.setPadding(dp(12),dp(3),dp(12),dp(5));hint.setBackgroundResource(R.drawable.sb_tint);
                add(hint,meta("Đã thống nhất với chủ đồ?"),5);
                add(hint,button("Tạo yêu cầu",true,()->openRequest(p,DEMO,3)),5);add(frame.footer,hint,0);
                compose(frame.footer,false,s->{if(vm.send(p,Message.text(s,true,ShareBoxViewModel.clock())))renderMessages(c,true);},
                        ()->pick(false),()->pick(true));
            }else note(frame.footer,"Bạn đã chặn người dùng này. Hãy bỏ chặn để tiếp tục.",R.color.sb_error);
        }
        private void renderMessages(Conversation c,boolean scroll){
            if(c.messages.isEmpty())empty(frame.content,"◇","Bắt đầu trò chuyện","Hãy hỏi chủ đồ về lịch trống và tiền đặt cọc.","",null);
            else {
                pageList(c.messages,1,this::bubble);
                if(scroll||memory.getBoolean("scrolled"))scrollLatest(c.messages.size());
            }
            memory.putBoolean("scrolled",true);
        }
        private void pick(boolean qr){qrSelection=qr;imagePicker.launch("image/*");}
        private void attach(Uri uri){
            if(uri==null||frame==null)return;
            Product p=product();if(p==null||vm.blocked(p))return;
            boolean qr=qrSelection;
            Message image=new Message(qr?"Mã QR":"Ảnh máy thực tế",true,ShareBoxViewModel.clock(),uri.toString(),null,qr);
            LinearLayout preview=col();ImageView pic=image(Art.CAMERA,uri.toString(),true);
            preview.addView(pic,new LinearLayout.LayoutParams(-1,dp(220)));
            if(qr)note(preview,QR_NOTE,R.color.sb_warning);
            new AlertDialog.Builder(requireContext()).setTitle(qr?"Gửi mã QR":"Gửi ảnh").setView(preview)
                    .setNegativeButton("Hủy",null).setPositiveButton("Gửi",(d,n)->{
                        if(vm.send(p,image))renderMessages(vm.chat(p),true);
                    }).show();
        }
    }

    public static final class Assistant extends ConversationPage{
        private boolean observed;
        public Assistant(){super(R.layout.screen_ai_chat);}
        @Override protected void render(){
            header("ShareBox AI");
            TextView introduction=meta("Trợ lý tự động · luôn sẵn sàng. Muốn thương lượng, hãy dùng Chat với chủ đồ.");
            introduction.setPadding(dp(16),dp(6),dp(16),dp(6));
            add(frame.above,introduction,4);
            chips(frame.footer,new String[]{"Thuê theo ngày hay theo tuần?","Mượn đồ hoạt động thế nào?","Tiền cọc là gì?","Gợi ý theo ngân sách"},-1,
                    i->vm.ask(new String[]{"Thuê theo ngày hay theo tuần?","Mượn đồ hoạt động thế nào?","Tiền cọc là gì?","Gợi ý theo ngân sách"}[i]));
            compose(frame.footer,true,vm::ask,null,null);
            if(!observed){
                observed=true;vm.changes().observe(getViewLifecycleOwner(),ignored->messages());
            }else messages();
        }
        private void messages(){
            if(frame==null)return;
            List<Object> rows=new ArrayList<>(state.assistant);
            int insert=Math.min(2,rows.size());
            for(int id:new int[]{7,3}){
                Product p=state.product(id);
                if(p!=null&&p.status==Status.ACTIVE)rows.add(insert++,p);
            }
            if(state.assistantBusy)rows.add(Message.text("•••  Đang trả lời",false,""));
            pageList(rows,1,row->{
                if(row instanceof Message)return bubble((Message)row);
                Product p=(Product)row;
                LinearLayout card=panel();add(card,productSummary(p,p.price()+" · Cọc "+money(p.deposit)),0);
                click(card,R.drawable.sb_card,()->open(R.id.product,p.id));return card;
            });
            if(state.assistant.size()>2)scrollLatest(rows.size());
        }
        @Override public void onDestroyView(){observed=false;super.onDestroyView();}
    }
}
