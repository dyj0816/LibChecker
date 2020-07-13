package com.absinthe.libchecker.recyclerview.adapter.snapshot

import android.content.res.ColorStateList
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isGone
import androidx.core.view.isVisible
import com.absinthe.libchecker.R
import com.absinthe.libchecker.bean.*
import com.absinthe.libchecker.utils.PackageUtils
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

const val ARROW = "→"

class SnapshotAdapter : BaseQuickAdapter<SnapshotDiffItem, BaseViewHolder>(R.layout.item_snapshot) {

    private val gson = Gson()

    override fun convert(holder: BaseViewHolder, item: SnapshotDiffItem) {
        val drawable = try {
            PackageUtils.getPackageInfo(item.packageName).applicationInfo.loadIcon(context.packageManager)
        } catch (e: Exception) {
            null
        }
        holder.setImageDrawable(R.id.iv_icon, drawable)

        var isNewOrDeleted = false

        when {
            item.deleted -> {
                holder.itemView.backgroundTintList =
                    ColorStateList.valueOf(
                        ContextCompat.getColor(
                            context,
                            R.color.material_red_300
                        )
                    )
                holder.setTextColor(
                    R.id.tv_version,
                    ContextCompat.getColor(context, R.color.textNormal)
                )
                holder.setTextColor(
                    R.id.tv_target_api,
                    ContextCompat.getColor(context, R.color.textNormal)
                )
                isNewOrDeleted = true
            }
            item.newInstalled -> {
                holder.itemView.backgroundTintList =
                    ColorStateList.valueOf(
                        ContextCompat.getColor(
                            context,
                            R.color.material_green_300
                        )
                    )
                holder.setTextColor(
                    R.id.tv_version,
                    ContextCompat.getColor(context, R.color.textNormal)
                )
                holder.setTextColor(
                    R.id.tv_target_api,
                    ContextCompat.getColor(context, R.color.textNormal)
                )
                isNewOrDeleted = true
            }
            else -> {
                holder.itemView.backgroundTintList = null
                holder.setTextColor(
                    R.id.tv_version,
                    ContextCompat.getColor(context, android.R.color.darker_gray)
                )
                holder.setTextColor(
                    R.id.tv_target_api,
                    ContextCompat.getColor(context, android.R.color.darker_gray)
                )
            }
        }

        if (isNewOrDeleted) {
            holder.getView<TextView>(R.id.indicator_added).isGone = true
            holder.getView<TextView>(R.id.indicator_removed).isGone = true
            holder.getView<TextView>(R.id.indicator_changed).isGone = true
        } else {
            val compareNode = compareNativeAndComponentDiff(item)
            holder.getView<TextView>(R.id.indicator_added).isVisible =
                compareNode.added and !isNewOrDeleted
            holder.getView<TextView>(R.id.indicator_removed).isVisible =
                compareNode.removed and !isNewOrDeleted
            holder.getView<TextView>(R.id.indicator_changed).isVisible =
                compareNode.changed and !isNewOrDeleted
            holder.getView<TextView>(R.id.indicator_moved).isVisible =
                compareNode.moved and !isNewOrDeleted
        }

        holder.setText(R.id.tv_app_name, getDiffString(item.labelDiff, isNewOrDeleted))
        holder.setText(R.id.tv_package_name, item.packageName)
        holder.setText(R.id.tv_version, getDiffString(item.versionNameDiff, item.versionCodeDiff, isNewOrDeleted, "%s (%s)"))
        holder.setText(R.id.tv_target_api, getDiffString(item.targetApiDiff, isNewOrDeleted, "API %s"))
        holder.setText(R.id.tv_abi, PackageUtils.getAbiString(item.abiDiff.old.toInt()))
        holder.setImageResource(R.id.iv_abi_type, PackageUtils.getAbiBadgeResource(item.abiDiff.old.toInt()))

        if (item.abiDiff.new != null && item.abiDiff.old != item.abiDiff.new) {
            holder.getView<TextView>(R.id.tv_arrow).isVisible = true

            val abiBadgeNewLayout = holder.getView<LinearLayout>(R.id.layout_abi_badge_new)
            abiBadgeNewLayout.isVisible = true
            abiBadgeNewLayout.findViewById<TextView>(R.id.tv_abi).text = PackageUtils.getAbiString(item.abiDiff.new.toInt())
            abiBadgeNewLayout.findViewById<ImageView>(R.id.iv_abi_type).setImageResource(PackageUtils.getAbiBadgeResource(item.abiDiff.new.toInt()))
        } else {
            holder.getView<TextView>(R.id.tv_arrow).isGone = true
            holder.getView<LinearLayout>(R.id.layout_abi_badge_new).isGone = true
        }
    }

    private fun compareNativeAndComponentDiff(item: SnapshotDiffItem): CompareDiffNode {
        val nativeCompareNode = compareNativeDiff(
            gson.fromJson(
                item.nativeLibsDiff.old,
                object : TypeToken<List<LibStringItem>>() {}.type
            ),
            gson.fromJson(
                item.nativeLibsDiff.new,
                object : TypeToken<List<LibStringItem>>() {}.type
            )
        )
        val servicesCompareNode = compareComponentsDiff(
            gson.fromJson(
                item.servicesDiff.old,
                object : TypeToken<List<String>>() {}.type
            ),
            gson.fromJson(
                item.servicesDiff.new,
                object : TypeToken<List<String>>() {}.type
            )
        )
        val activitiesCompareNode = compareComponentsDiff(
            gson.fromJson(
                item.activitiesDiff.old,
                object : TypeToken<List<String>>() {}.type
            ),
            gson.fromJson(
                item.activitiesDiff.new,
                object : TypeToken<List<String>>() {}.type
            )
        )
        val receiversCompareNode = compareComponentsDiff(
            gson.fromJson(
                item.receiversDiff.old,
                object : TypeToken<List<String>>() {}.type
            ),
            gson.fromJson(
                item.receiversDiff.new,
                object : TypeToken<List<String>>() {}.type
            )
        )
        val providersCompareNode = compareComponentsDiff(
            gson.fromJson(
                item.providersDiff.old,
                object : TypeToken<List<String>>() {}.type
            ),
            gson.fromJson(
                item.providersDiff.new,
                object : TypeToken<List<String>>() {}.type
            )
        )

        val totalNode =
            CompareDiffNode()
        totalNode.added =
            nativeCompareNode.added or servicesCompareNode.added or activitiesCompareNode.added or receiversCompareNode.added or providersCompareNode.added
        totalNode.removed =
            nativeCompareNode.removed or servicesCompareNode.removed or activitiesCompareNode.removed or receiversCompareNode.removed or providersCompareNode.removed
        totalNode.changed =
            nativeCompareNode.changed or servicesCompareNode.changed or activitiesCompareNode.changed or receiversCompareNode.changed or providersCompareNode.changed
        totalNode.moved =
            nativeCompareNode.moved or servicesCompareNode.moved or activitiesCompareNode.moved or receiversCompareNode.moved or providersCompareNode.moved

        return totalNode
    }

    private fun compareNativeDiff(
        oldList: List<LibStringItem>,
        newList: List<LibStringItem>?
    ): CompareDiffNode {
        if (newList == null) {
            return CompareDiffNode(
                removed = true
            )
        }

        val tempOldList = oldList.toMutableList()
        val tempNewList = newList.toMutableList()
        val sameList = mutableListOf<LibStringItem>()
        val node =
            CompareDiffNode()

        for (item in tempNewList) {
            oldList.find { it.name == item.name }?.let {
                if (it.size != item.size) {
                    node.changed = true
                }
                sameList.add(item)
            }
        }

        for (item in sameList) {
            tempOldList.remove(tempOldList.find { it.name == item.name })
            tempNewList.remove(tempNewList.find { it.name == item.name })
        }

        if (tempOldList.isNotEmpty()) {
            node.removed = true
        }
        if (tempNewList.isNotEmpty()) {
            node.added = true
        }
        return node
    }

    private fun compareComponentsDiff(
        oldList: List<String>,
        newList: List<String>?
    ): CompareDiffNode {
        if (newList == null) {
            return CompareDiffNode(
                removed = true
            )
        }

        val tempOldList = oldList.toMutableList()
        val tempNewList = newList.toMutableList()
        val sameList = mutableListOf<String>()
        val node =
            CompareDiffNode()

        for (item in tempNewList) {
            oldList.find { it == item }?.let {
                sameList.add(item)
            }
        }

        for (item in sameList) {
            tempOldList.remove(item)
            tempNewList.remove(item)
        }

        var simpleName: String
        val deletedOldList = mutableListOf<String>()
        val deletedNewList = mutableListOf<String>()

        for (item in tempNewList) {
            simpleName = item.substringAfterLast(".")
            tempOldList.find { it.substringAfterLast(".") == simpleName }?.let {
                node.moved = true
                deletedOldList.add(it)
                deletedNewList.add(item)
            }
        }
        tempOldList.removeAll(deletedOldList)
        tempNewList.removeAll(deletedNewList)

        if (tempOldList.isNotEmpty()) {
            node.removed = true
        }
        if (tempNewList.isNotEmpty()) {
            node.added = true
        }
        return node
    }

    private fun <T> getDiffString(diff: SnapshotDiffItem.DiffNode<T>, isNewOrDeleted: Boolean = false, format: String = "%s"): String {
        return if (diff.old != diff.new && !isNewOrDeleted) {
            "${String.format(format, diff.old.toString())} $ARROW ${String.format(format, diff.new.toString())}"
        } else {
            String.format(format, diff.old.toString())
        }
    }

    private fun getDiffString(diff1: SnapshotDiffItem.DiffNode<*>, diff2: SnapshotDiffItem.DiffNode<*>, isNewOrDeleted: Boolean = false, format: String = "%s"): String {
        return if ((diff1.old != diff1.new || diff2.old != diff2.new) && !isNewOrDeleted) {
            "${String.format(format, diff1.old.toString(), diff2.old.toString())} $ARROW ${String.format(format, diff1.new.toString(), diff2.new.toString())}"
        } else {
            String.format(format, diff1.old.toString(), diff2.old.toString())
        }
    }

    data class CompareDiffNode(
        var added: Boolean = false,
        var removed: Boolean = false,
        var changed: Boolean = false,
        var moved: Boolean = false
    )
}