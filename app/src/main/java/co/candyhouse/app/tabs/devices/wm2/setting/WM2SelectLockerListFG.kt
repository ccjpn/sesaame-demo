package co.candyhouse.app.tabs.devices.wm2.setting

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import co.candyhouse.app.R
import co.candyhouse.app.base.BaseDeviceFG
import co.candyhouse.sesame.open.device.CHDevices
import co.candyhouse.sesame.open.device.CHWifiModule2Delegate
import co.utils.SharedPreferencesUtils
import co.utils.recycle.GenericAdapter
import kotlinx.android.synthetic.main.fg_wm2_scan_list.*

class WM2SelectLockerListFG : BaseDeviceFG(R.layout.fg_wm2_select_locker_list), CHWifiModule2Delegate {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        leaderboard_list.apply {
            adapter = object : GenericAdapter<CHDevices>(mDeviceModel.myChDevices.value.filter { it.productModel.isLocker() }) {
                override fun getLayoutId(position: Int, obj: CHDevices): Int = R.layout.key_cell
                override fun getViewHolder(view: View, viewType: Int): RecyclerView.ViewHolder = object : RecyclerView.ViewHolder(view), Binder<CHDevices> {
                    var title = itemView.findViewById<TextView>(R.id.title)
                    var wifi_img = itemView.findViewById<View>(R.id.wifi_img)
                    override fun bind(locker: CHDevices, pos: Int) {
                        wifi_img.visibility = View.GONE
                        title.text = locker.deviceId.toString()
                        itemView.setOnClickListener {
                            mDeviceModel.wm2LiveData.value!!.insertSesames(locker) {
                                it.onSuccess {
                                    SharedPreferencesUtils.preferences.edit().putString(locker.deviceId.toString(), title.text.toString()).apply()
                                    findNavController().navigateUp()
                                }
                            }
                        }
                    }
                }
            }
        }//end leaderboard_list.apply
    }

}