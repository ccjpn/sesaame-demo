package co.candyhouse.app.tabs.devices.wm2.setting

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.observe
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import co.candyhouse.app.BuildConfig
import co.candyhouse.app.R
import co.candyhouse.app.base.BaseDeviceFG
import co.utils.L
import co.utils.SharedPreferencesUtils
import co.utils.alertview.AlertView
import co.utils.alertview.enums.AlertActionStyle
import co.utils.alertview.enums.AlertStyle
import co.utils.alertview.fragments.toastMSG
import co.utils.alertview.objects.AlertAction
import co.utils.recycle.GenericAdapter
import kotlinx.android.synthetic.main.fg_wm2_setting.*
import co.utils.alerts.ext.inputTextAlert
import co.candyhouse.app.base.BleStatusUpdate
import co.candyhouse.app.tabs.devices.ssm2.*
import co.candyhouse.sesame.open.CHBleManager
import co.candyhouse.sesame.open.CHBleStatusDelegate
import co.candyhouse.sesame.open.device.*

class WM2SettingFG : BaseDeviceFG(R.layout.fg_wm2_setting), CHWifiModule2Delegate, BleStatusUpdate {

    var mDeviceList = ArrayList<String>()
    var versionTag: MutableLiveData<String> = MutableLiveData()

    override fun onResume() {
        super.onResume()
        L.d("hcia", "WM2SettingFG onResume:")
        onChange()

        CHBleManager.statusDelegate = object : CHBleStatusDelegate {
            override fun didScanChange(ss: CHScanStatus) {
//                L.d("hcia", "ss:" + ss)
                onChange()
            }
        }
        mDeviceModel.wm2Delegates[mDeviceModel.wm2LiveData.value!!] = object : CHWifiModule2Delegate {
            override fun onSSM2KeysChanged(device: CHWifiModule2, ssm2keys: Map<String, String>) {
                mDeviceList.clear()
                mDeviceList.addAll(mDeviceModel.wm2LiveData.value!!.ssm2KeysMap.map { it.key })

                L.d("hcia", "mDeviceList:" + mDeviceList)
                recy?.post {
                    (recy.adapter as GenericAdapter<*>).notifyDataSetChanged()
                    devices_empty_logo.visibility = if (mDeviceList.size == 0) View.VISIBLE else View.GONE
                }
            }

            override fun onAPSettingChanged(device: CHWifiModule2, settings: CHWifiModule2MechSettings) {
                wifi_ssid_txt?.text = settings.wifiSSID
                wifi_pass_txt?.text = settings.wifiPassWord
            }

            override fun onNetWorkStatusChanged(device: CHWifiModule2, status: CHWifiModule2NetWorkStatus) {
                ap_icon?.isSelected = status.isAPWork == true
                iot_ok_line?.visibility = if (status.isIOTWork== true) View.VISIBLE else View.GONE
                net_ok_line?.visibility = if (status.isNetWork == true) View.VISIBLE else View.GONE

                ap_icon?.isSelected = status.isAPWork == true
                net_icon?.isSelected = status.isNetWork == true
                iot_icon?.isSelected = status.isIOTWork == true

                aping_icon?.visibility = if (status.isAPConnecting) View.VISIBLE else View.GONE
                neting_icon?.visibility = if (status.isConnectingNet) View.VISIBLE else View.GONE
                iot_ing_icon?.visibility = if (status.isConnectingIOT) View.VISIBLE else View.GONE
                ssid_logo?.visibility = if (status.isAPCheck == false) View.VISIBLE else View.GONE

            }

            override fun onBleDeviceStatusChanged(device: CHWifiModule2, status: CHSesame2Status) {
                L.d("hcia", "🎒 UI wm2 連線狀態 status:" + status)
                if (status.value == CHDeviceLoginStatus.Login) {
                    mDeviceModel.wm2LiveData.value?.getVersionTag { versionTag.postValue("version:" + it.getOrNull()?.data) }

                }
                if (device.deviceStatus == CHSesame2Status.ReceivedBle) {
                    device.connect { }
                }
                onChange()
            }

            override fun onOTAProgress(device: CHWifiModule2, percent: Byte) {
                wm2_version_txt?.post { wm2_version_txt?.text = percent.toString() }
            }
        }

    }

    @SuppressLint("SimpleDateFormat")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        L.d("hcia", "onViewCreated:")


        reset_zone.visibility = if (BuildConfig.DEBUG) View.VISIBLE else View.GONE

        mDeviceModel.wm2LiveData.observe(viewLifecycleOwner, { wm2 ->
            wm2.connect { }
            wm2_id_txt.text = wm2.deviceId.toString()
            wifi_ssid_txt.text = wm2.mechSetting?.wifiSSID
            wifi_pass_txt.text = wm2.mechSetting?.wifiPassWord
            ap_icon.isSelected = (wm2.networkStatus?.isAPWork == true)
            net_icon.isSelected = (wm2.networkStatus?.isNetWork == true)
            iot_icon.isSelected = (wm2.networkStatus?.isIOTWork == true)
            ssid_logo?.visibility = if (wm2.networkStatus?.isAPCheck == false) View.VISIBLE else View.GONE
            iot_ok_line?.visibility = if (iot_icon.isSelected) View.VISIBLE else View.GONE
            net_ok_line?.visibility = if (net_icon.isSelected) View.VISIBLE else View.GONE
            wm2.getVersionTag { versionTag.postValue("version:" + it.getOrNull()?.data) }

        })
        versionTag.observe(viewLifecycleOwner) { wm2_version_txt.text = it }

        dfu_zone.setOnClickListener {
            AlertView("", "", AlertStyle.IOS).apply {
                addAction(AlertAction(getString(R.string.wm2_update), AlertActionStyle.NEGATIVE) { action ->
                    mDeviceModel.wm2LiveData.value?.updateFirmware {}
                })
                show(activity as AppCompatActivity)
            }
        }
        recy.apply {
            mDeviceList.clear()
            mDeviceList.addAll(mDeviceModel.wm2LiveData.value!!.ssm2KeysMap.map { it.key })
            devices_empty_logo.visibility = if (mDeviceList.size == 0) View.VISIBLE else View.GONE

            adapter = object : GenericAdapter<String>(mDeviceList) {
                override fun getLayoutId(position: Int, obj: String): Int = R.layout.wm2_key_cell

                override fun getViewHolder(view: View, viewType: Int): RecyclerView.ViewHolder = object : RecyclerView.ViewHolder(view), Binder<String> {
                    var title = itemView.findViewById<TextView>(R.id.title)
                    override fun bind(keyId: String, pos: Int) {
                        title.text = keyId
                        itemView.setOnClickListener {
                            AlertView(title.text.toString(), "", AlertStyle.IOS).apply {
                                addAction(AlertAction(getString(R.string.ssm_delete), AlertActionStyle.NEGATIVE) { action ->
                                    mDeviceModel.wm2LiveData.value!!.removeSesame(keyId) {}
                                })
                                show(activity as AppCompatActivity)
                            }
                        }
                    }
                }
            }
        }



        ap_scan_zone.setOnClickListener { findNavController().navigate(R.id.action_WM2SettingFG_to_WM2ScanFG) }
        add_locker_zone.setOnClickListener { findNavController().navigate(R.id.action_WM2SettingFG_to_WM2SelectLockerFG) }

        drop_zone.setOnClickListener {
            mDeviceModel.wm2LiveData.value!!.dropKey {
                findNavController().navigateUp()
                mDeviceModel.updateDevices()

            }
        }
        reset_zone.setOnClickListener {
            mDeviceModel.wm2LiveData.value!!.reset {
                findNavController().navigateUp()
                mDeviceModel.updateDevices()
            }
        }

    }//end view created

    override fun onChange() {
        when {
            CHBleManager.mScanning == CHScanStatus.BleClose -> {
                view?.findViewById<View>(R.id.err_zone)?.visibility = View.VISIBLE
                view?.findViewById<TextView>(R.id.err_title)?.text = getString(R.string.noble)
//            getView()?.findViewById<TextView>(R.id.err_msg)?.setText("qqqq")
            }
            mDeviceModel.targetDevice().deviceStatus == CHSesame2Status.NoBleSignal -> {
                view?.findViewById<View>(R.id.err_zone)?.visibility = View.VISIBLE
                view?.findViewById<TextView>(R.id.err_title)?.text = getString(R.string.NoBleSignal)
            }
            mDeviceModel.targetDevice().deviceStatus.value == CHDeviceLoginStatus.UnLogin -> {
                view?.findViewById<View>(R.id.err_zone)?.visibility = View.VISIBLE
                view?.findViewById<TextView>(R.id.err_title)?.text = mDeviceModel.targetDevice().deviceStatus.toString()
            }
            else -> {
                view?.findViewById<View>(R.id.err_zone)?.visibility = View.GONE
            }
        }

    }


}