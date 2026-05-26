package snd.komf.providers.yenpress

class YenPressEmptySeriesException(seriesId: String) :
    IllegalStateException("No books found for YenPress series $seriesId")
