package frb.astrostar.manager.features.quicksettings

import android.graphics.drawable.Icon
import android.service.quicksettings.Tile
import android.widget.Toast
import frb.astrostar.manager.R

class AstroStarTileService : android.service.quicksettings.TileService() {

    override fun onStartListening() {
        super.onStartListening()
        qsTile.apply {
            label = "AxTest"
            icon = Icon.createWithResource(this@AstroStarTileService, R.drawable.ic_astrostar)
            state = Tile.STATE_INACTIVE
            updateTile()
        }


    }

    override fun onClick() {
        super.onClick()
        Toast.makeText(this, "Clicked", Toast.LENGTH_SHORT).show()
    }


}