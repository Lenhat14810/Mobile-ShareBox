package com.example.sharebox;

import android.os.Bundle;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.espresso.NoMatchingViewException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.espresso.matcher.ViewMatchers.withHint;
import static androidx.test.espresso.matcher.ViewMatchers.withContentDescription;
import static org.hamcrest.CoreMatchers.allOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static com.example.sharebox.data.Models.*;

@RunWith(AndroidJUnit4.class)
public final class ShareBoxInstrumentedTest {
    @Rule public final ActivityScenarioRule<MainActivity> activity=new ActivityScenarioRule<>(MainActivity.class);

    @Test public void onboardingAuthenticationAndFeatureDestinations() {
        shown("Khám phá đồ dùng");
        tap("Tiếp tục   ›");
        shown("Kết nối dễ dàng");
        tap("Tiếp tục   ›");
        shown("Chia sẻ thông minh");
        tap("Bắt đầu ngay");
        shown("Chào mừng trở lại");
        tap("Đăng nhập");
        tap("G   Tiếp tục với Google");
        shown("Sản phẩm nổi bật");
        tap("Khám phá");
        shown("7 kết quả");
        tap("Tin nhắn");
        shown("Chat");
        tap("Cá nhân");
        shown("Sản phẩm của tôi");
        tap("Sản phẩm của tôi");
        shown("Tất cả 6");
        onView(allOf(withContentDescription("Quay lại"),isDisplayed())).perform(click());
        shown("Cài đặt");

        int[] routes={R.id.product,R.id.chat,R.id.ai_chat,R.id.tx_request,R.id.transactions,R.id.tx_detail,
                R.id.payment,R.id.finance,R.id.notifications,R.id.wishlist,R.id.edit_profile,R.id.my_products,
                R.id.create_product,R.id.settings,R.id.states};
        String[] headers={"Máy ảnh Canon EOS 200D kèm lens 18-55mm","Trần Quốc Lâm","AI",
                "Yêu cầu thuê / mượn","Giao dịch của tôi","Chi tiết giao dịch","Số tiền cần thanh toán","Tài chính",
                "Thông báo","Yêu thích","Chỉnh sửa hồ sơ","Sản phẩm của tôi","Đăng tin mới","Cài đặt & hỗ trợ","Không thể kết nối"};
        for(int i=0;i<routes.length;i++){
            final int route=routes[i];
            activity.getScenario().onActivity(a->{
                Bundle args=new Bundle();
                args.putInt("id",route==R.id.product||route==R.id.chat||route==R.id.tx_request||
                        route==R.id.tx_detail||route==R.id.payment?1:0);
                a.open(route,args);
            });
            shown(headers[i]);
            activity.getScenario().onActivity(a->a.back());
        }
    }

    @Test public void requestSubmissionReusesChatAndListsPending() {
        activity.getScenario().onActivity(a->{
            a.reset(R.id.home);
            Bundle args=new Bundle();
            args.putInt("id",1);
            a.open(R.id.tx_request,args);
        });
        shown("Yêu cầu thuê / mượn");
        tap("Gửi yêu cầu");
        shown("Xem giao dịch của tôi");
        activity.getScenario().onActivity(a->{
            assertEquals(5,a.model.state.chats.size());
            Transaction created=a.model.state.transactions.get(0);
            assertEquals(1,created.productId);
            assertEquals(Stage.PENDING,created.stage());
            assertNotNull(a.model.state.chats.get(1).latest());
        });
        tap("Xem giao dịch của tôi");
        shown("Giao dịch của tôi");
    }

    @Test public void searchFinanceThemeAndRoleConfirmations() {
        activity.getScenario().onActivity(a->{
            a.model.state.assistantHidden=true;
            a.reset(R.id.home);
        });
        tap("Khám phá");
        onView(withHint("Tìm theo tên hoặc từ khóa")).perform(replaceText("may anh"));
        shown("1 kết quả");
        tap("Máy ảnh Canon EOS 200D");
        shown("Máy ảnh Canon EOS 200D kèm lens 18-55mm");
        pressBack();
        tap("Cá nhân");
        tap("Tài chính");
        shown("4.820.000đ");
        tap("Quý này");
        pressBack();
        tap("Cài đặt");
        onView(withContentDescription("Chế độ tối")).perform(click());
        shown("Cài đặt & hỗ trợ");
        activity.getScenario().onActivity(a->org.junit.Assert.assertTrue(a.model.state.dark));
        onView(withContentDescription("Chế độ tối")).perform(click());
        shown("Cài đặt & hỗ trợ");

        activity.getScenario().onActivity(a->{
            Bundle b=new Bundle();
            b.putInt("id",1);
            a.open(R.id.tx_detail,b);
        });
        confirm("Mô phỏng chủ đồ: Duyệt yêu cầu");
        tap("Xem thanh toán & đặt cọc");
        tap("Tiền mặt");
        confirm("Tôi đã thanh toán");
        stage(Stage.PAYMENT);
        confirm("Mô phỏng chủ đồ: Xác nhận đã nhận tiền");
        stage(Stage.HANDOVER);
        tap("Về chi tiết giao dịch");
        confirm("Tôi đã nhận đồ");
        stage(Stage.HANDOVER);
        confirm("Mô phỏng chủ đồ: Xác nhận đã giao đồ");
        stage(Stage.ACTIVE);
        confirm("Xác nhận đã trả đồ");
        stage(Stage.RETURN);
        tap("Mô phỏng chủ đồ: Xác nhận đã nhận lại");
        onView(withHint("0")).perform(replaceText("150000"));
        tap("Xác nhận");
        stage(Stage.COMPLETE);
        activity.getScenario().onActivity(a->assertEquals(850000L,a.model.state.transaction(1).refund()));
    }

    private void stage(Stage expected){
        activity.getScenario().onActivity(a->assertEquals(expected,a.model.state.transaction(1).stage()));
    }
    private static void confirm(String label){
        tap(label);
        tap("Xác nhận");
    }

    private static void tap(String label) {
        onView(allOf(withText(label),isDisplayed())).perform(click());
    }
    private static void shown(String label) {
        long until=android.os.SystemClock.uptimeMillis()+12000;
        NoMatchingViewException last=null;
        do{
            try{
                onView(allOf(withText(label),isDisplayed())).check(matches(isDisplayed()));
                return;
            }catch(NoMatchingViewException wait){last=wait;android.os.SystemClock.sleep(150);}
        }while(android.os.SystemClock.uptimeMillis()<until);
        throw last;
    }
}
