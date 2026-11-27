package com.example.sharebox;

import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
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
import com.example.sharebox.ui.ProfilePages;
import com.example.sharebox.ui.ShareBoxViewModel;

public final class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;
    private NavController nav;
    private NavHostFragment navHost;
    public ShareBoxViewModel model;
    private int current = R.id.splash, tab = R.id.home;
    private boolean keyboard;

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
        navHost=(NavHostFragment)getSupportFragmentManager().findFragmentById(R.id.nav_host);
        if(navHost==null)throw new IllegalStateException("Thiếu bộ điều hướng");
        nav=navHost.getNavController();
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
    }
    public boolean showsHomeAiShortcut(){
        return getPreferences(MODE_PRIVATE).getBoolean("home_ai_shortcut",false);
    }
    public void setHomeAiShortcut(boolean enabled){
        getPreferences(MODE_PRIVATE).edit().putBoolean("home_ai_shortcut",enabled).apply();
    }
    private boolean isTab(int id){return id==R.id.home||id==R.id.explore||id==R.id.chats||id==R.id.profile;}
    private boolean bottom(int id){
        return isTab(id)||id==R.id.create_product||id==R.id.finance||id==R.id.transactions||id==R.id.notifications||
                id==R.id.wishlist||id==R.id.my_products||id==R.id.settings||id==R.id.states;
    }
    private void chrome(){
        if(binding==null||model==null)return;
        int navVisibility=!keyboard&&bottom(current)?View.VISIBLE:View.GONE;
        binding.bottomNav.setVisibility(navVisibility);
        navIcon(binding.navHome,R.drawable.sb_ic_home,R.id.home);
        navIcon(binding.navExplore,R.drawable.sb_ic_explore,R.id.explore);
        navIcon(binding.navCreate,R.drawable.sb_ic_plus,R.id.create_product);
        navIcon(binding.navChat,R.drawable.sb_ic_chat,R.id.chats);
        navIcon(binding.navProfile,R.drawable.sb_ic_profile,R.id.profile);

    }
    private void navIcon(TextView v,int icon,int destination){
        boolean selected=current==R.id.create_product?destination==R.id.create_product:tab==destination;
        int tint=ContextCompat.getColor(this,selected?R.color.sb_accent:R.color.sb_muted);
        Drawable d=ContextCompat.getDrawable(this,icon);
        if(d!=null){d=d.mutate();d.setTintList(ColorStateList.valueOf(tint));d.setBounds(0,0,dp(22),dp(22));v.setCompoundDrawables(null,d,null,null);}
        v.setPadding(dp(4),dp(7),dp(4),dp(5));
        v.setCompoundDrawablePadding(dp(3));
        v.setTextColor(tint);
        v.setTypeface(Typeface.DEFAULT,selected?Typeface.BOLD:Typeface.NORMAL);
        v.setSelected(selected);
    }
    private int dp(int px){return (int)(px*getResources().getDisplayMetrics().density+.5f);}
    public void open(int destination,Bundle args){
        if(destination==current&&args==null)return;
        NavOptions.Builder options=new NavOptions.Builder().setLaunchSingleTop(true);
        if(current==R.id.create_product&&destination==R.id.my_products){
            var previous=nav.getPreviousBackStackEntry();
            if(previous!=null&&previous.getDestination().getId()==destination){
                nav.popBackStack();
                return;
            }
            options.setPopUpTo(R.id.create_product,true);
        }
        nav.navigate(destination,args,options.build());
    }
    public void openTab(int destination){
        var screen=navHost.getChildFragmentManager().getPrimaryNavigationFragment();
        if(current==R.id.create_product&&screen instanceof ProfilePages.Create){
            ((ProfilePages.Create)screen).requestLeave(()->openTabAfterDiscard(destination));
            return;
        }
        openTabAfterDiscard(destination);
    }
    private void openTabAfterDiscard(int destination){
        // Remove the temporary route before saving the outgoing tab stack.
        if(current==R.id.create_product)nav.popBackStack();
        if(tab==destination&&current==destination)return;
        if(tab==destination&&isTab(destination)&&nav.popBackStack(destination,false))return;
        tab=destination;
        nav.navigate(destination,null,new NavOptions.Builder().setPopUpTo(R.id.home,false,true)
                .setRestoreState(true).setLaunchSingleTop(true).build());
        // Tab state written by an older app version may still contain a listing draft.
        if(current==R.id.create_product)nav.popBackStack();
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
}
