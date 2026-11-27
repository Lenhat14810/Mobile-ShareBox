package com.example.sharebox.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.sharebox.MainActivity;
import com.example.sharebox.R;
import com.example.sharebox.databinding.CardProductBinding;
import com.example.sharebox.databinding.ComponentFieldBinding;
import com.example.sharebox.databinding.ComponentGradientBannerBinding;
import com.example.sharebox.databinding.ComponentPanelBinding;
import com.example.sharebox.databinding.ScreenFrameBinding;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntConsumer;
import java.util.function.IntPredicate;
import static com.example.sharebox.data.Models.*;

public abstract class Page extends Fragment {
    private static final ExecutorService IMAGES=Executors.newFixedThreadPool(2);
    protected ScreenFrameBinding frame;
    protected MainActivity host;
    protected ShareBoxViewModel vm;
    protected State state;
    protected final Bundle memory=new Bundle();
    protected static final int WHITE=0xffffffff;
    protected static final int PINK=0xffea5578;

    protected Page(int layout){super(layout);}
    @Override public final void onViewCreated(@NonNull View view,@Nullable Bundle saved) {
        super.onViewCreated(view,saved);
        frame=ScreenFrameBinding.bind(view.findViewById(R.id.screen_frame));
        host=(MainActivity)requireActivity();
        vm=host.model;state=vm.state;
        if(saved!=null && saved.getBundle("inputs")!=null)memory.putAll(saved.getBundle("inputs"));
        render();
    }
    protected abstract void render();
    protected void refresh(){
        if(frame==null)return;
        int top=frame.scroll.getScrollY();
        frame.header.removeAllViews();frame.header.setVisibility(View.GONE);
        frame.above.removeAllViews();frame.content.removeAllViews();frame.footer.removeAllViews();
        frame.scroll.setVisibility(View.VISIBLE);frame.items.setVisibility(View.GONE);frame.items.setAdapter(null);
        render();
        frame.scroll.post(()->{if(frame!=null)frame.scroll.scrollTo(0,top);});
    }
    @Override public void onSaveInstanceState(@NonNull Bundle out){out.putBundle("inputs",memory);super.onSaveInstanceState(out);}
    @Override public void onDestroyView(){frame=null;super.onDestroyView();}
    protected int dp(float n){return Math.round(n*getResources().getDisplayMetrics().density);}
    protected int color(int res){return ContextCompat.getColor(requireContext(),res);}
    protected Bundle args(){return getArguments()==null?Bundle.EMPTY:getArguments();}
    protected int itemId(){return args().getInt("id",0);}
    protected Product product(){return state.product(itemId());}
    protected Transaction transaction(){return state.transaction(itemId());}
    protected void open(int route){host.open(route,null);}
    protected void open(int route,int id){Bundle b=new Bundle();b.putInt("id",id);host.open(route,b);}
    protected void openRequest(Product p,LocalDate start,int days){
        if(vm.blocked(p)){shortNote("Bạn không thể gửi yêu cầu cho người dùng này.");return;}
        vm.chat(p);
        Bundle b=new Bundle();b.putInt("id",p.id);b.putLong("start",start.toEpochDay());b.putInt("days",days);
        host.open(R.id.tx_request,b);
    }
    protected void openChat(Product p){
        Conversation c=vm.chat(p);c.unread=0;vm.changed();open(R.id.chat,p.id);
    }
    protected void back(){if(!host.back())host.openTab(R.id.home);}
    protected String screenTitle(){
        TextView title=requireView().findViewById(R.id.title_config);
        return title==null?"":title.getText().toString();
    }
    protected void header(){header(screenTitle());}
    protected void header(String title) {
        LinearLayout row=frame.header;
        row.setVisibility(View.VISIBLE);
        row.addView(icon(R.drawable.sb_ic_back,"Quay lại",this::back));
        TextView label=text(title,20,R.color.sb_ink,true);
        LinearLayout.LayoutParams width=new LinearLayout.LayoutParams(0,-2,1);
        width.leftMargin=dp(8);
        row.addView(label,width);
    }
    protected LinearLayout col(){LinearLayout l=new LinearLayout(requireContext());l.setOrientation(LinearLayout.VERTICAL);return l;}
    protected LinearLayout row(){LinearLayout l=new LinearLayout(requireContext());l.setGravity(Gravity.CENTER_VERTICAL);return l;}
    protected void add(LinearLayout l,View v,int top){
        LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(l.getOrientation()==LinearLayout.VERTICAL?-1:-2,-2);
        params.topMargin=dp(top);l.addView(v,params);
    }
    protected void weight(LinearLayout row,View view){row.addView(view,new LinearLayout.LayoutParams(0,-2,1));}
    protected TextView text(CharSequence s,float size,int tone,boolean bold){
        TextView t=new TextView(requireContext());
        t.setText(s);t.setTextSize(size);t.setTextColor(color(tone));t.setIncludeFontPadding(false);
        t.setLineSpacing(dp(2),1f);
        if(bold)t.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));
        return t;
    }
    protected TextView title(String s){return text(s,18,R.color.sb_ink,true);}
    protected TextView body(String s){return text(s,14,R.color.sb_ink,false);}
    protected TextView meta(String s){return text(s,12,R.color.sb_muted,false);}
    protected TextView tag(String s,int tone){
        TextView t=text(s,12,tone,true);t.setPadding(dp(9),dp(5),dp(9),dp(5));t.setBackgroundResource(R.drawable.sb_tint);return t;
    }
    protected TextView button(String s,boolean primary,Runnable run){
        TextView t=text(s,14,primary?R.color.sb_paper:R.color.sb_ink,true);
        if(primary)t.setTextColor(WHITE);
        t.setMinHeight(dp(48));t.setGravity(Gravity.CENTER);
        t.setPadding(dp(13),dp(8),dp(13),dp(8));
        click(t,primary?R.drawable.sb_blue:R.drawable.sb_card,run);
        return t;
    }
    protected TextView textAction(String label,Runnable action){
        TextView link=text(label,14,R.color.sb_accent,true);
        link.setGravity(Gravity.CENTER);
        link.setMinHeight(dp(48));
        click(link,0,action);
        link.setPaddingRelative(dp(8),0,dp(8),0);
        return link;
    }
    protected ImageButton icon(int asset,String label,Runnable run){
        return icon(asset,label,true,run);
    }
    protected ImageButton icon(int asset,String label,boolean framed,Runnable run){
        ImageButton b=new ImageButton(requireContext());
        b.setImageResource(asset);b.setImageTintList(ColorStateList.valueOf(color(R.color.sb_ink)));
        b.setLayoutParams(new LinearLayout.LayoutParams(dp(48),dp(48)));
        b.setPadding(dp(12),dp(12),dp(12),dp(12));b.setScaleType(ImageView.ScaleType.FIT_CENTER);b.setContentDescription(label);
        click(b,framed?R.drawable.sb_card:0,run);
        return b;
    }
    protected void click(View target,int background,Runnable action){
        target.setBackground(new RippleDrawable(ColorStateList.valueOf(0x24265de6),
                background==0?null:ContextCompat.getDrawable(requireContext(),background),shape(WHITE,14)));
        target.setClickable(true);target.setFocusable(true);
        target.setOnClickListener(v->action.run());
    }
    protected GradientDrawable shape(int color,int radius){
        GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(dp(radius));return g;
    }
    protected LinearLayout panel(){
        LinearLayout p=ComponentPanelBinding.inflate(getLayoutInflater()).getRoot();p.setElevation(dp(2));return p;
    }
    protected void details(LinearLayout p,String label,String value){
        LinearLayout line=row();TextView name=meta(label);weight(line,name);
        TextView amount=text(value,14,R.color.sb_ink,true);amount.setGravity(Gravity.END);line.addView(amount);
        add(p,line,8);
    }
    protected void space(LinearLayout p,int height){
        View v=new View(requireContext());p.addView(v,new LinearLayout.LayoutParams(1,dp(height)));
    }
    protected void note(LinearLayout p,String message,int tone){
        TextView t=text(message,13,tone,false);t.setBackgroundResource(R.drawable.sb_tint);
        t.setPadding(dp(12),dp(10),dp(12),dp(10));add(p,t,10);
    }
    protected void footer(String s,boolean primary,Runnable click){
        frame.footer.setPadding(dp(16),dp(3),dp(16),dp(8));add(frame.footer,button(s,primary,click),5);
    }
    protected void banner(LinearLayout p,String heading,String title,String subtitle) {
        ComponentGradientBannerBinding card=ComponentGradientBannerBinding.inflate(getLayoutInflater(),p,false);
        card.kicker.setText(heading);
        card.headline.setText(title);
        card.subtitle.setText(subtitle);
        card.subtitle.setVisibility(subtitle.isEmpty()?View.GONE:View.VISIBLE);
        add(p,card.getRoot(),6);
    }
    protected void chips(LinearLayout parent,String[] options,int current,Consumer<Integer> choose){
        HorizontalScrollView scroll=new HorizontalScrollView(requireContext());
        scroll.setHorizontalScrollBarEnabled(false);
        LinearLayout row=row();
        for(int i=0;i<options.length;i++){
            final int n=i;
            TextView t=button(options[i],i==current,()->choose.accept(n));
            t.setTextSize(13);t.setBackground(new RippleDrawable(ColorStateList.valueOf(0x20265de6),
                    shape(i==current?color(R.color.sb_blue):color(R.color.sb_paper),28),shape(WHITE,28)));
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-2,-2);lp.setMarginEnd(dp(8));row.addView(t,lp);
        }
        scroll.addView(row);add(parent,scroll,8);
        if(current>2)scroll.post(()->{if(current<row.getChildCount())scroll.smoothScrollTo(row.getChildAt(current).getLeft(),0);});
    }
    protected void flatFilters(LinearLayout parent,String[] labels,IntPredicate selected,IntConsumer choose){
        HorizontalScrollView scroll=new HorizontalScrollView(requireContext());
        scroll.setHorizontalScrollBarEnabled(false);
        LinearLayout options=row();
        int firstSelected=-1;
        for(int i=0;i<labels.length;i++){
            int index=i;
            TextView option=text(labels[i],13,R.color.sb_muted,false);
            option.setGravity(Gravity.CENTER);
            option.setMinHeight(dp(48));
            option.setMinWidth(dp(48));
            click(option,0,()->{
                choose.accept(index);
                for(int j=0;j<options.getChildCount();j++){
                    showFilterSelection((TextView)options.getChildAt(j),selected.test(j));
                }
            });
            option.setPaddingRelative(dp(10),0,dp(10),0);
            boolean active=selected.test(i);
            showFilterSelection(option,active);
            if(active&&firstSelected<0)firstSelected=i;
            options.addView(option,new LinearLayout.LayoutParams(-2,-2));
        }
        scroll.addView(options);
        add(parent,scroll,0);
        if(firstSelected>=3){
            View selectedOption=options.getChildAt(firstSelected);
            scroll.post(()->scroll.scrollTo(selectedOption.getLeft(),0));
        }
    }
    private void showFilterSelection(TextView option,boolean selected){
        option.setSelected(selected);
        option.setTextColor(color(selected?R.color.sb_accent:R.color.sb_muted));
        option.setTypeface(Typeface.DEFAULT,selected?Typeface.BOLD:Typeface.NORMAL);
        option.setPaintFlags(selected?option.getPaintFlags()|android.graphics.Paint.UNDERLINE_TEXT_FLAG:
                option.getPaintFlags()&~android.graphics.Paint.UNDERLINE_TEXT_FLAG);
    }
    protected AppCompatEditText field(LinearLayout p,String title,String hint,String value,int type,int lines){
        ComponentFieldBinding field=ComponentFieldBinding.inflate(getLayoutInflater(),p,false);
        field.fieldTitle.setText(title);
        field.fieldTitle.setVisibility(title.isEmpty()?View.GONE:View.VISIBLE);
        field.fieldInput.setInputType(type);
        field.fieldInput.setSingleLine(lines==1);
        field.fieldInput.setMinLines(lines);
        if(lines>1)field.fieldInput.setGravity(Gravity.TOP);
        field.fieldInput.setHint(hint);
        field.fieldInput.setText(value);
        add(p,field.getRoot(),12);
        return field.fieldInput;
    }
    protected LinearLayout searchBar(LinearLayout parent,String hint,String initial,Consumer<String> change,Consumer<String> enter) {
        LinearLayout box=row();
        box.setBackgroundResource(R.drawable.sb_card);
        AppCompatEditText input=new AppCompatEditText(requireContext());
        input.setSingleLine(true);
        input.setTextSize(14);
        input.setMinHeight(dp(50));
        input.setMinWidth(0);
        input.setPaddingRelative(dp(2),dp(8),dp(2),dp(8));
        input.setBackgroundColor(0x00000000);
        input.setHint(hint);
        input.setTextColor(color(R.color.sb_ink));
        input.setHintTextColor(color(R.color.sb_muted));
        input.setText(initial);
        input.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH);
        ImageButton lens=icon(R.drawable.sb_ic_search,"Tìm kiếm",()->{});
        click(lens,0,()->{
            if(enter!=null)enter.accept(value(input));
            else if(input.requestFocus()){
                InputMethodManager manager=requireContext().getSystemService(InputMethodManager.class);
                if(manager!=null)manager.showSoftInput(input,InputMethodManager.SHOW_IMPLICIT);
            }
        });
        box.addView(lens);
        weight(box,input);
        ImageButton clear=icon(R.drawable.sb_ic_close,"Xóa tìm kiếm",()->{});
        click(clear,0,()->input.setText(""));
        clear.setVisibility(initial.isEmpty()?View.INVISIBLE:View.VISIBLE);
        box.addView(clear);
        watch(input,value->{
            clear.setVisibility(value.isEmpty()?View.INVISIBLE:View.VISIBLE);
            change.accept(value);
        });
        input.setOnEditorActionListener((v,action,event)->{
            if(action==android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH){
                if(enter!=null)enter.accept(value(input));keyboardHide(input);return true;
            }
            return false;
        });
        add(parent,box,4);
        return box;
    }
    protected void watch(EditText input,Consumer<String> callback){
        input.addTextChangedListener(new TextWatcher(){
            @Override public void beforeTextChanged(CharSequence s,int start,int count,int after){}
            @Override public void onTextChanged(CharSequence s,int start,int before,int count){callback.accept(s.toString());}
            @Override public void afterTextChanged(Editable s){}
        });
    }
    protected String value(EditText e){return e.getText()==null?"":e.getText().toString().trim();}
    protected long number(EditText e){
        try{return Long.parseLong(value(e));}catch(NumberFormatException e1){return 0;}
    }
    protected void keyboardHide(View input){
        InputMethodManager manager=(InputMethodManager)requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if(manager!=null)manager.hideSoftInputFromWindow(input.getWindowToken(),0);
    }
    protected void empty(LinearLayout p,String glyph,String title,String message,String action,Runnable run){
        LinearLayout group=col();group.setGravity(Gravity.CENTER);group.setPadding(dp(12),dp(36),dp(12),dp(20));
        TextView icon=text(glyph,28,R.color.sb_accent,true);icon.setGravity(Gravity.CENTER);icon.setBackground(shape(color(R.color.sb_tint),100));
        group.addView(icon,new LinearLayout.LayoutParams(dp(76),dp(76)));
        TextView h=title(title);h.setGravity(Gravity.CENTER);add(group,h,14);
        TextView desc=meta(message);desc.setGravity(Gravity.CENTER);add(group,desc,8);
        if(run!=null)add(group,button(action,true,run),13);
        add(p,group,8);
    }
    protected void confirm(String title,String detail,String action,Runnable run){
        new AlertDialog.Builder(requireContext()).setTitle(title).setMessage(detail).setNegativeButton("Hủy",null)
                .setPositiveButton(action,(dialog,n)->run.run()).show();
    }
    protected void choose(String title,String[] options,Consumer<Integer> action){
        new AlertDialog.Builder(requireContext()).setTitle(title).setItems(options,(d,n)->action.accept(n))
                .setNegativeButton("Đóng",null).show();
    }
    protected void shortNote(String text){Toast.makeText(requireContext(),text,Toast.LENGTH_SHORT).show();}
    protected TextView avatar(String initials){
        TextView v=text(initials,18,R.color.sb_accent,true);v.setGravity(Gravity.CENTER);
        v.setBackground(shape(color(R.color.sb_tint),100));v.setLayoutParams(new LinearLayout.LayoutParams(dp(48),dp(48)));return v;
    }
    protected int artId(Art a){
        switch(a){
            case DRILL:return R.drawable.drill_art;case LAPTOP:return R.drawable.laptop_art;case CHAIR:return R.drawable.chair_art;
            case SPEAKER:return R.drawable.speaker_art;case BOOK:return R.drawable.book_art;case BIKE:return R.drawable.sb_ic_explore;
            default:return R.drawable.camera_art;
        }
    }
    protected int artBg(Art a){
        switch(a){
            case DRILL:return 0xffe2f4ec;case LAPTOP:return 0xffdbe5fc;case SPEAKER:return 0xffe9eefe;
            case CHAIR:case BIKE:return 0xffe7ebf6;default:return 0xfffef1db;
        }
    }
    protected ImageView image(Art a,String uri,boolean large){
        ImageView image=new ImageView(requireContext());
        int padding=dp(large?25:8);image.setPadding(padding,padding,padding,padding);image.setBackground(shape(artBg(a),12));
        image.setScaleType(ImageView.ScaleType.FIT_CENTER);load(image,uri,artId(a));return image;
    }
    protected void load(ImageView target,String source,int fallback) {
        target.setTag(source);
        target.setImageResource(fallback);
        if(source==null||source.isEmpty())return;
        Context ctx=target.getContext().getApplicationContext();
        IMAGES.execute(()->{
            Bitmap result=null;
            try {
                Uri uri=Uri.parse(source);
                if(Build.VERSION.SDK_INT>=28){
                    result=ImageDecoder.decodeBitmap(ImageDecoder.createSource(ctx.getContentResolver(),uri),(decoder,info,d)->{
                        decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE);
                        decoder.setTargetSampleSize(Math.max(1,Math.max(info.getSize().getWidth(),info.getSize().getHeight())/1000));
                    });
                } else {
                    BitmapFactory.Options options=new BitmapFactory.Options();options.inSampleSize=2;
                    try(InputStream in=ctx.getContentResolver().openInputStream(uri)){result=BitmapFactory.decodeStream(in,null,options);}
                }
            }catch(Exception ignored){ /* Keep the product illustration when a document is no longer accessible. */ }
            final Bitmap bitmap=result;
            target.post(()->{if(bitmap!=null&&source.equals(target.getTag()))target.setImageBitmap(bitmap);});
        });
    }
    protected String cover(Product p){return p.images.isEmpty()?null:p.images.get(0);}
    protected View productCard(Product p,boolean grid,boolean wish,Runnable repaint){
        CardProductBinding b=CardProductBinding.inflate(getLayoutInflater());
        if(!grid){
            b.getRoot().setOrientation(LinearLayout.HORIZONTAL);
            b.artFrame.setLayoutParams(new LinearLayout.LayoutParams(dp(124),dp(140)));
            b.caption.setLayoutParams(new LinearLayout.LayoutParams(0,-2,1));
            b.name.setMinLines(1);
        }
        b.artFrame.setBackgroundColor(artBg(p.art));
        load(b.image,cover(p),artId(p.art));b.image.setContentDescription(p.name);
        b.hot.setVisibility(p.hot()?View.VISIBLE:View.GONE);
        b.newTag.setVisibility(p.recent()?View.VISIBLE:View.GONE);
        b.name.setText(p.name);b.price.setText(p.price());b.price.setTextColor(color(p.rate==0?R.color.sb_success:R.color.sb_accent));
        b.deposit.setText(p.deposit>0?"Cọc "+money(p.deposit):"Không cần cọc");
        b.location.setText("⌖  "+p.location);b.quantity.setText("◇  Số lượng: "+p.quantity);
        bindFavorite(b.favorite,p,wish?repaint:null);
        b.chatAction.setVisibility(wish?View.VISIBLE:View.GONE);
        b.chatAction.setOnClickListener(v->openChat(p));
        click(b.getRoot(),R.drawable.sb_card,()->open(R.id.product,p.id));
        return b.getRoot();
    }
    protected void bindFavorite(ImageButton view,Product product,Runnable repaint){
        var owner=getViewLifecycleOwner();
        Observer<State> show=updated->{
            boolean selected=updated.favorites.contains(product.id);
            view.setImageResource(selected?R.drawable.sb_ic_heart_filled:R.drawable.sb_ic_heart);
            view.setImageTintList(ColorStateList.valueOf(selected?PINK:color(R.color.sb_ink)));
            view.setSelected(selected);
            view.setContentDescription(selected?"Bỏ yêu thích":"Yêu thích");
        };
        show.onChanged(state);
        view.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener(){
            @Override public void onViewAttachedToWindow(View v){vm.changes().observe(owner,show);}
            @Override public void onViewDetachedFromWindow(View v){vm.changes().removeObserver(show);}
        });
        if(view.isAttachedToWindow())vm.changes().observe(owner,show);
        click(view,R.drawable.sb_card,()->{
            vm.favorite(product.id);
            if(repaint!=null)repaint.run();
        });
    }
    protected View productSummary(Product p,String second){
        LinearLayout row=row();ImageView pic=image(p.art,cover(p),false);
        row.addView(pic,new LinearLayout.LayoutParams(dp(60),dp(60)));
        LinearLayout words=col();words.setPadding(dp(12),0,0,0);
        add(words,text(p.name,14,R.color.sb_ink,true),0);add(words,meta(second),4);
        weight(row,words);return row;
    }
    protected <T> void list(RecyclerView list,List<T> data,int columns,Function<T,View> factory){
        list.setLayoutManager(new GridLayoutManager(requireContext(),columns));
        list.setAdapter(new Rows<>(data,factory,dp(5)));
    }
    protected <T> void embedded(LinearLayout parent,List<T> data,int columns,Function<T,View> factory){
        RecyclerView list=new RecyclerView(requireContext());list.setNestedScrollingEnabled(false);list.setClipToPadding(false);
        list(list,data,columns,factory);add(parent,list,4);
    }
    protected <T> void pageList(List<T> data,int columns,Function<T,View> factory){
        frame.items.setVisibility(View.VISIBLE);frame.scroll.setVisibility(View.GONE);
        list(frame.items,data,columns,factory);
    }
    protected String date(LocalDate date){return date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));}
    protected void missing(String title){header(title);empty(frame.content,"!",title,"Vui lòng quay lại và thử lại.","Quay lại",this::back);}
    protected static final class Rows<T> extends RecyclerView.Adapter<Rows.Holder>{
        final List<T> data;final Function<T,View> factory;final int margin;
        Rows(List<T> data,Function<T,View> factory,int margin){this.data=new ArrayList<>(data);this.factory=factory;this.margin=margin;}
        @NonNull @Override public Holder onCreateViewHolder(@NonNull ViewGroup parent,int viewType){
            FrameLayout container=new FrameLayout(parent.getContext());
            RecyclerView.LayoutParams lp=new RecyclerView.LayoutParams(-1,-2);lp.setMargins(margin,margin,margin,margin);
            container.setLayoutParams(lp);return new Holder(container);
        }
        @Override public void onBindViewHolder(@NonNull Holder holder,int pos){
            holder.container.removeAllViews();holder.container.addView(factory.apply(data.get(pos)),new FrameLayout.LayoutParams(-1,-2));
        }
        @Override public int getItemCount(){return data.size();}
        static final class Holder extends RecyclerView.ViewHolder {
            final FrameLayout container;
            Holder(FrameLayout container){super(container);this.container=container;}
        }
    }
}
