package com.example.sharebox;

import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.NavOptions;
import androidx.navigation.fragment.NavHostFragment;
import com.example.sharebox.databinding.ActivityMainBinding;
import com.example.sharebox.ui.ShareBoxViewModel;

public final class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;
    private NavController nav;
    public ShareBoxViewModel model;
    private int current = R.id.splash, tab = R.id.home;
    private boolean keyboard;
    private final Handler timer = new Handler(Looper.getMainLooper());
    private final String[] tips={"Cần thuê hay mượn đồ gì? Hỏi tôi nhé!", "Thử hỏi: laptop thuê dưới 500.000đ?", "Bạn muốn tôi giúp gì hôm nay?"};
    private int tip;
    private final Runnable rotateTip=new Runnable(){
        @Override public void run(){ if(binding==null)return;binding.suggestion.setText(tips[++tip%tips.length]); timer.postDelayed(this,6000); }
    };

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        model=new ViewModelProvider(this).get(ShareBoxViewModel.class);
        if(state!=null)tab=state.getInt("tab",R.id.home);
        binding=ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        WindowCompat.setDecorFitsSystemWindows(getWindow(),false);
        WindowCompat.getInsetsController(getWindow(),binding.root).setAppearanceLightStatusBars(!model.state.dark);
        WindowCompat.getInsetsController(getWindow(),binding.root).setAppearanceLightNavigationBars(!model.state.dark);
        ViewCompat.setOnApplyWindowInsetsListener(binding.root,(view,insets)->{
            var bars=insets.getInsets(WindowInsetsCompat.Type.systemBars()|WindowInsetsCompat.Type.displayCutout());
            var ime=insets.getInsets(WindowInsetsCompat.Type.ime());
            keyboard=insets.isVisible(WindowInsetsCompat.Type.ime());
            view.setPadding(bars.left,bars.top,bars.right,Math.max(bars.bottom,ime.bottom));
            chrome();
            return WindowInsetsCompat.CONSUMED;
        });
        NavHostFragment host=(NavHostFragment)getSupportFragmentManager().findFragmentById(R.id.nav_host);
        if(host==null)throw new IllegalStateException("Thiếu bộ điều hướng");
        nav=host.getNavController();
        nav.addOnDestinationChangedListener((controller,destination,args)->{
            current=destination.getId();
            if(isTab(current))tab=current;
            chrome();
        });
        binding.navHome.setOnClickListener(v->openTab(R.id.home));
        binding.navExplore.setOnClickListener(v->openTab(R.id.explore));
        binding.navChat.setOnClickListener(v->openTab(R.id.chats));
        binding.navProfile.setOnClickListener(v->openTab(R.id.profile));
        binding.navCreate.setOnClickListener(v->{if(current!=R.id.create_product)open(R.id.create_product,null);});
        binding.navCreate.setContentDescription("Đăng tin mới");
        binding.suggestion.setOnClickListener(v->open(R.id.ai_chat,null));
        binding.robot.setOnClickListener(v->open(R.id.ai_chat,null));
        binding.assistantClose.setOnClickListener(v->{model.state.assistantHidden=true;chrome();});
        binding.robot.setOnTouchListener(new View.OnTouchListener(){
            float x,y,oldX,oldY;
            boolean moved;
            @Override public boolean onTouch(View v,MotionEvent event){
                switch(event.getActionMasked()){
                    case MotionEvent.ACTION_DOWN:
                        x=event.getRawX();y=event.getRawY();oldX=binding.assistant.getTranslationX();oldY=binding.assistant.getTranslationY();moved=false;return true;
                    case MotionEvent.ACTION_MOVE:
                        float dx=event.getRawX()-x,dy=event.getRawY()-y;
                        moved|=Math.abs(dx)+Math.abs(dy)>12;
                        if(moved){
                            View b=binding.assistant;
                            b.setTranslationX(clamp(oldX+dx,-b.getLeft(),binding.root.getWidth()-b.getRight()));
                            b.setTranslationY(clamp(oldY+dy,-b.getTop(),binding.root.getHeight()-b.getBottom()));
                        }
                        return true;
                    case MotionEvent.ACTION_UP:
                        if(!moved)v.performClick();
                        return true;
                    default:return true;
                }
            }
        });
        timer.postDelayed(rotateTip,6000);
    }
    private float clamp(float n,float min,float max){return Math.max(min,Math.min(max,n));}
    private boolean isTab(int id){return id==R.id.home||id==R.id.explore||id==R.id.chats||id==R.id.profile;}
    private boolean bottom(int id){
        return isTab(id)||id==R.id.create_product||id==R.id.finance||id==R.id.transactions||id==R.id.notifications||
                id==R.id.wishlist||id==R.id.my_products||id==R.id.settings||id==R.id.states;
    }
    private void chrome(){
        if(binding==null||model==null)return;
        int navVisibility=!keyboard&&bottom(current)?View.VISIBLE:View.GONE;
        binding.bottomNav.setVisibility(navVisibility);
        binding.navCreate.setVisibility(navVisibility);
        boolean intro=current==R.id.splash||current==R.id.onboarding||current==R.id.auth;
        binding.assistant.setVisibility(!intro&&!keyboard&&current!=R.id.ai_chat&&!model.state.assistantHidden?View.VISIBLE:View.GONE);
        navIcon(binding.navHome,R.drawable.sb_ic_home,R.id.home);
        navIcon(binding.navExplore,R.drawable.sb_ic_explore,R.id.explore);
        navIcon(binding.navChat,R.drawable.sb_ic_chat,R.id.chats);
        navIcon(binding.navProfile,R.drawable.sb_ic_profile,R.id.profile);

    }
    private void navIcon(TextView v,int icon,int destination){
        int tint=ContextCompat.getColor(this,tab==destination?R.color.sb_accent:R.color.sb_muted);
        Drawable d=ContextCompat.getDrawable(this,icon);
        if(d!=null){d=d.mutate();d.setTintList(ColorStateList.valueOf(tint));d.setBounds(0,0,dp(22),dp(22));v.setCompoundDrawables(null,d,null,null);}
        v.setPadding(0,dp(7),0,dp(5));v.setTextColor(tint);
        v.setSelected(tab==destination);
    }
    private int dp(int px){return (int)(px*getResources().getDisplayMetrics().density+.5f);}
    public void open(int destination,Bundle args){
        if(destination==current&&args==null)return;
        nav.navigate(destination,args,new NavOptions.Builder().setLaunchSingleTop(true).build());
    }
    public void openTab(int destination){
        if(tab==destination&&current==destination)return;
        if(tab==destination&&isTab(destination)&&nav.popBackStack(destination,false))return;
        tab=destination;
        nav.navigate(destination,null,new NavOptions.Builder().setPopUpTo(R.id.home,false,true)
                .setRestoreState(true).setLaunchSingleTop(true).build());
    }
    public void reset(int destination){
        nav.navigate(destination,null,new NavOptions.Builder().setPopUpTo(R.id.sharebox_graph,true)
                .setLaunchSingleTop(true).build());
    }
    public boolean back(){return nav.popBackStack();}
    public void dark(boolean dark){
        if(model.state.dark==dark)return;
        model.state.dark=dark;model.changed();
        AppCompatDelegate.setDefaultNightMode(dark?AppCompatDelegate.MODE_NIGHT_YES:AppCompatDelegate.MODE_NIGHT_NO);
    }
    @Override protected void onSaveInstanceState(Bundle out){out.putInt("tab",tab);super.onSaveInstanceState(out);}
    @Override protected void onDestroy(){timer.removeCallbacksAndMessages(null);binding=null;super.onDestroy();}
}
