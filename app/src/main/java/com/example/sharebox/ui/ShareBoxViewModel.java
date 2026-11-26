package com.example.sharebox.ui;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import android.os.Handler;
import android.os.Looper;
import com.example.sharebox.data.MockShareBoxRepository;
import com.example.sharebox.data.ShareBoxRepository;
import static com.example.sharebox.data.Models.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;

public final class ShareBoxViewModel extends ViewModel {
    private final ShareBoxRepository repository = new MockShareBoxRepository();
    public final State state = repository.demoState();
    private final MutableLiveData<State> live = new MutableLiveData<>(state);
    private final ScheduledExecutorService worker=Executors.newSingleThreadScheduledExecutor();
    private final Handler main=new Handler(Looper.getMainLooper());
    public LiveData<State> changes(){ return live; }
    public void changed(){ live.setValue(state); }
    public void authenticate(String email,String name,String password,boolean registration,Runnable complete){
        worker.execute(()->{
            repository.authenticate(email,password,registration);
            main.postDelayed(()->{
                Profile p=state.profile;
                state.profile=new Profile(name.isEmpty()?p.name:name,email.isEmpty()?p.email:email,p.phone,p.area,p.bio,p.photo);
                state.signedIn=true;
                changed();complete.run();
            },700);
        });
    }
    public void ask(String input){
        String question=input.trim();
        if(question.isEmpty() || state.assistantBusy)return;
        state.assistant.add(Message.text(question,true,clock()));
        state.assistantBusy=true;changed();
        worker.schedule(()->{
            String answer=repository.answer(question);
            main.post(()->{
                state.assistantBusy=false;
                state.assistant.add(Message.text(answer,false,clock()));
                changed();
            });
        },1400,TimeUnit.MILLISECONDS);
    }
    public boolean blocked(Product p){return state.blocked.contains(p.owner);}
    public Conversation chat(Product p){
        Conversation chat=state.chats.get(p.id);
        if(chat==null){
            chat=new Conversation(p.id,p.owner,initials(p.owner),0);
            state.chats.put(p.id,chat);
        }
        return chat;
    }
    public boolean send(Product p,Message m){
        if(blocked(p) || (m.text.trim().isEmpty()&&m.uri==null&&m.art==null&&!m.qr))return false;
        chat(p).messages.add(m);
        changed();
        return true;
    }
    public void favorite(int id){
        if(!state.favorites.remove(id))state.favorites.add(id);
        changed();
    }
    public void listing(Product p){
        for(int i=0;i<state.products.size();i++){
            if(state.products.get(i).id==p.id){state.products.set(i,p);changed();return;}
        }
        state.products.add(0,p);changed();
    }
    public int nextProduct(){return state.products.stream().mapToInt(p->p.id).max().orElse(0)+1;}
    public Transaction request(Product p,LocalDate start,int days,boolean deposit){
        if(blocked(p)||!state.available(p.id,start,days))return null;
        int id=state.transactions.stream().mapToInt(t->t.id).max().orElse(0)+1;
        long rate=p.id==1?220000:p.rate;
        Transaction t=new Transaction(id,p.id,p.owner,Role.RENTER,start,days,rate,
                p.usage()==Usage.RENT||deposit?p.deposit:0);
        state.transactions.add(0,t);
        chat(p).messages.add(Message.text("Đã gửi yêu cầu "+(p.usage()==Usage.RENT?"thuê":"mượn")+" "+days+" ngày.",true,clock()));
        notice(NoticeType.REQUEST,"Đã gửi yêu cầu",p.name,t.id);
        changed();return t;
    }
    public boolean confirm(Transaction t,Step step,Role role) {
        if(step==Step.ACCEPT&&!state.available(t.productId,t.start,t.days))return false;
        if(!t.confirm(step,role))return false;
        Product p=state.product(t.productId);
        String name=p==null?"Giao dịch":p.name;
        if(step==Step.RECEIVED_MONEY){
            boolean owner=t.myRole==Role.OWNER;
            if(t.fee()>0)state.entries.add(0,new Entry(name,owner?t.fee():-t.fee(),owner?EntryType.INCOME:EntryType.EXPENSE,DEMO));
            if(t.deposit>0)state.entries.add(0,new Entry("Cọc · "+name,t.deposit,EntryType.HELD,DEMO));
            notice(NoticeType.PAYMENT,"Đã xác nhận thanh toán",name,t.id);
        }
        if(t.stage()==Stage.COMPLETE){
            if(t.deposit>0) {
                state.entries.add(0,new Entry("Giải phóng cọc · "+name,-t.deposit,EntryType.HELD,DEMO));
                if(t.refund()>0)state.entries.add(0,new Entry("Hoàn cọc · "+name,-t.refund(),EntryType.REFUND,DEMO));
                if(t.deduction>0)state.entries.add(0,new Entry("Khấu trừ · "+name,-t.deduction,EntryType.DEDUCTION,DEMO));
            }
            notice(NoticeType.STATUS,"Giao dịch hoàn tất",name,t.id);
        }
        changed();return true;
    }
    private void notice(NoticeType kind,String title,String description,int target) {
        int id=state.notices.stream().mapToInt(n->n.id).max().orElse(0)+1;
        state.notices.add(0,new Notice(id,kind,title,description,clock(),target,false));
    }
    public static String clock(){return LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));}
    @Override protected void onCleared(){worker.shutdownNow();main.removeCallbacksAndMessages(null);}
}
